/*
 * Copyright (c) 2015 RWTH Aachen. All rights reserved.
 *
 * http://www.se-rwth.de/
 */
package org.nest.codegeneration;

import de.monticore.generating.GeneratorEngine;
import de.monticore.generating.GeneratorSetup;
import de.monticore.generating.templateengine.GlobalExtensionManagement;
import de.se_rwth.commons.Names;
import org.apache.commons.io.FileUtils;
import org.nest.codegeneration.converters.GSLReferenceConverter;
import org.nest.codegeneration.converters.NESTReferenceConverter;
import org.nest.codegeneration.helpers.*;
import org.nest.codegeneration.printers.NESTMLDynamicsPrinter;
import org.nest.codegeneration.printers.NESTMLFunctionPrinter;
import org.nest.codegeneration.sympy.ODEProcessor;
import org.nest.nestml._ast.ASTBodyDecorator;
import org.nest.nestml._ast.ASTNESTMLCompilationUnit;
import org.nest.nestml._ast.ASTNeuron;
import org.nest.nestml._parser.NESTMLParser;
import org.nest.nestml._symboltable.NESTMLScopeCreator;
import org.nest.nestml.prettyprinter.NESTMLPrettyPrinter;
import org.nest.nestml.prettyprinter.NESTMLPrettyPrinterFactory;
import org.nest.spl._ast.ASTOdeDeclaration;
import org.nest.spl.prettyprinter.ExpressionsPrettyPrinter;
import org.nest.utils.ASTNodes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static de.se_rwth.commons.Names.getPathFromPackage;
import static de.se_rwth.commons.logging.Log.info;

/**
 * Generates C++ Implementation and model integration code for NEST.
 * @author plotnikov
 */
public class NESTML2NESTCodeGenerator {
  private final String LOG_NAME = NESTML2NESTCodeGenerator.class.getName();
  private final ODEProcessor odeProcessor = new ODEProcessor();
  private final NESTReferenceConverter converter;
  private final ExpressionsPrettyPrinter expressionsPrinter;
  private final NESTMLScopeCreator scopeCreator;
  private final NESTMLParser parser;

  public NESTML2NESTCodeGenerator(
      final NESTMLScopeCreator scopeCreator) {
    this.scopeCreator = scopeCreator;
    converter = new NESTReferenceConverter();
    expressionsPrinter = new ExpressionsPrettyPrinter(converter);
    parser = new NESTMLParser();
  }

  public void analyseAndGenerate(
      final ASTNESTMLCompilationUnit root,
      final Path outputBase) {
    info("Starts processing of the model: " + ASTNodes.toString(root.getPackageName()),
        LOG_NAME);
    final ASTNESTMLCompilationUnit workingVersion;

    workingVersion = computeSolutionForODE(root, scopeCreator, outputBase);
    generateNESTCode(workingVersion, outputBase);

    info("Successfully generated NEST code for: " + ASTNodes.toString(root.getPackageName()),
        LOG_NAME);
  }

  public ASTNESTMLCompilationUnit computeSolutionForODE(
      final ASTNESTMLCompilationUnit root,
      final NESTMLScopeCreator scopeCreator,
      final Path outputBase) {
    final String moduleName = ASTNodes.toString(root.getPackageName());
    final Path modulePath = Paths.get(outputBase.toString(), getPathFromPackage(moduleName));

    ASTNESTMLCompilationUnit withSolvedOde = odeProcessor.process(root, modulePath);

    return printAndReadModel(scopeCreator, modulePath, withSolvedOde);

  }

  /**
   * This action is done for 2 Reasons:
   * a) Technically it is necessary to build a new symbol table
   * b) The model developer can view how the
   * @return New root node of the altered model with an initialized symbol table
   */
  private ASTNESTMLCompilationUnit printAndReadModel(
      final NESTMLScopeCreator scopeCreator,
      final Path modulePath,
      final ASTNESTMLCompilationUnit root) {
    try {
      final Path outputTmpPath = Paths.get(modulePath.toString(), "tmp.nestml");
      printModelToFile(root, outputTmpPath.toString());
      final ASTNESTMLCompilationUnit withSolvedOde = parser.parseNESTMLCompilationUnit
          (outputTmpPath.toString()).get();
      scopeCreator.runSymbolTableCreator(withSolvedOde);
      return withSolvedOde;
    }
    catch (IOException e) {
      throw  new RuntimeException(e);
    }
  }

  protected void generateNESTCode(ASTNESTMLCompilationUnit workingVersion, Path outputBase) {
    generateHeader(workingVersion, outputBase);
    generateClassImplementation(workingVersion, outputBase);
    generateNestModuleCode(workingVersion, outputBase);
  }

  public void generateHeader(
      final ASTNESTMLCompilationUnit compilationUnit,
      final Path outputFolder) {
    final String moduleName = Names.getQualifiedName(compilationUnit.getPackageName().getParts());

    final GeneratorSetup setup = new GeneratorSetup(new File(outputFolder.toString()));
    final GlobalExtensionManagement glex = getGlexConfiguration();
    setup.setGlex(glex);

    final GeneratorEngine generator = new GeneratorEngine(setup);

    for (ASTNeuron neuron : compilationUnit.getNeurons()) {
      setNeuronGenerationParameter(glex, neuron, moduleName);
      final Path outputFile = Paths.get(getPathFromPackage(moduleName), neuron.getName() + ".h");

      // TODO: how do I find out the call was successful?
      generator.generate("org.nest.nestml.neuron.NeuronHeader", outputFile, neuron);
    }
    
  }

  public void generateClassImplementation(
      final ASTNESTMLCompilationUnit compilationUnit,
      final Path outputDirectory) {
    final String moduleName = Names.getQualifiedName(compilationUnit.getPackageName().getParts());

    final GeneratorSetup setup = new GeneratorSetup(new File(outputDirectory.toString()));
    final GlobalExtensionManagement glex = getGlexConfiguration();
    setup.setGlex(glex);

    final GeneratorEngine generator = new GeneratorEngine(setup);

    final List<ASTNeuron> neurons = compilationUnit.getNeurons();
    for (ASTNeuron neuron : neurons) {
      setNeuronGenerationParameter(glex, neuron, moduleName);
      final Path classImplementationFile = Paths.get(
          getPathFromPackage(moduleName), neuron.getName() + ".cpp");
      // TODO: how do I find out the call was successful?
      generator.generate(
          "org.nest.nestml.neuron.NeuronClass",
          classImplementationFile,
          neuron);
    }

  }

  public void generateNestModuleCode(
      final ASTNESTMLCompilationUnit compilationUnit,
      final Path outputDirectory) {
    final String fullName = Names.getQualifiedName(compilationUnit.getPackageName().getParts());
    final String moduleName = Names.getSimpleName(fullName);

    final List<ASTNeuron> neurons = compilationUnit.getNeurons();
    final List<String> neuronModelNames = neurons
        .stream()
        .map(ASTNeuron::getName)
        .collect(Collectors.toList());

    final GeneratorSetup setup = new GeneratorSetup(new File(outputDirectory.toString()));
    setup.setTracing(false);

    final GlobalExtensionManagement glex = getGlexConfiguration();
    glex.setGlobalValue("moduleName", moduleName);
    glex.setGlobalValue("packageName", fullName);
    glex.setGlobalValue("neuronModelNames", neuronModelNames);

    setup.setGlex(glex);

    final GeneratorEngine generator = new GeneratorEngine(setup);

    final Path makefileFile = Paths.get(getPathFromPackage(fullName), "Makefile.am");
    generator.generate(
        "org.nest.nestml.module.Makefile",
        makefileFile,
        compilationUnit);

    final Path bootstrappingFile = Paths.get(getPathFromPackage(fullName), "bootstrap.sh");
    generator.generate(
        "org.nest.nestml.module.Bootstrap",
        bootstrappingFile,
        compilationUnit);

    final Path configureFile = Paths.get(getPathFromPackage(fullName), "configure.ac");
    generator.generate(
        "org.nest.nestml.module.Configure",
        configureFile,
        compilationUnit);

    final Path moduleClass = Paths.get(getPathFromPackage(fullName), moduleName + "Config.cpp");
    generator.generate(
        "org.nest.nestml.module.ModuleClass",
        moduleClass,
        compilationUnit);

    final Path moduleHeader = Paths.get(getPathFromPackage(fullName), moduleName + "Config.h");
    generator.generate(
        "org.nest.nestml.module.ModuleHeader",
        moduleHeader,
        compilationUnit);

    final Path sliInitFile = Paths.get(
        getPathFromPackage(fullName), "sli", moduleName.toLowerCase() + "-init");
    generator.generate(
        "org.nest.nestml.module.SLI_Init",
        sliInitFile,
        compilationUnit);
  }

  private void printModelToFile(
      final ASTNESTMLCompilationUnit root,
      final String outputFolderBase) {
    final NESTMLPrettyPrinter prettyPrinter = NESTMLPrettyPrinterFactory.createNESTMLPrettyPrinter();
    root.accept(prettyPrinter);

    final File prettyPrintedModelFile = new File(outputFolderBase);
    try {
      FileUtils.write(prettyPrintedModelFile, prettyPrinter.getResult());
    }
    catch (IOException e) {
      throw new RuntimeException("Cannot write the prettyprinted model to the file: " + outputFolderBase, e);
    }

  }

  public GlobalExtensionManagement getGlexConfiguration() {
    final GlobalExtensionManagement glex = new GlobalExtensionManagement();
    glex.setGlobalValue("expressionsPrinter", expressionsPrinter);
    glex.setGlobalValue("functionCallConverter", converter);
    return glex;
  }


  private void setNeuronGenerationParameter(
      final GlobalExtensionManagement glex,
      final ASTNeuron neuron,
      final String moduleName) {
    setSolverType(glex, neuron);

    final String guard = (moduleName + "." + neuron.getName()).replace(".", "_");
    glex.setGlobalValue("guard", guard);
    glex.setGlobalValue("simpleNeuronName", neuron.getName());

    final String nspPrefix = convertToCppNamespaceConvention(moduleName);
    final NESTMLFunctionPrinter functionPrinter = new NESTMLFunctionPrinter();
    final NESTMLDeclarations declarations = new NESTMLDeclarations();
    final NESTMLDynamicsPrinter dynamicsHelper = new NESTMLDynamicsPrinter();

    glex.setGlobalValue("declarations", new NESTMLDeclarations() );
    glex.setGlobalValue("assignmentHelper", new SPLVariableGetterSetterHelper());
    glex.setGlobalValue("functionPrinter", functionPrinter);
    glex.setGlobalValue("declarations", declarations);
    glex.setGlobalValue("dynamicsHelper", dynamicsHelper);
    glex.setGlobalValue("bufferHelper", new NESTMLBuffers());

    glex.setGlobalValue("nspPrefix", nspPrefix);
    glex.setGlobalValue("outputEvent", NESTMLOutputs.printOutputEvent(neuron));
    glex.setGlobalValue("isOutputEventPresent", NESTMLOutputs.isOutputEventPresent(neuron));
    glex.setGlobalValue("isSpikeInput", NESTMLInputs.isSpikeInput(neuron));
    glex.setGlobalValue("isCurrentInput", NESTMLInputs.isCurrentInput(neuron));
    glex.setGlobalValue("body", new ASTBodyDecorator(neuron.getBody()));

    final GSLReferenceConverter converter = new GSLReferenceConverter();
    final ExpressionsPrettyPrinter expressionsPrinter = new ExpressionsPrettyPrinter(converter);
    glex.setGlobalValue("expressionsPrinterForGSL", expressionsPrinter);

  }

  private void setSolverType(GlobalExtensionManagement glex, ASTNeuron neuron) {
    final ASTBodyDecorator astBodyDecorator = new ASTBodyDecorator(neuron.getBody());
    glex.setGlobalValue("useGSL", false);
    if (astBodyDecorator.getOdeDefinition().isPresent()) {
      if (astBodyDecorator.getOdeDefinition().get().getODEs().size() > 1) {
        glex.setGlobalValue("useGSL", true);
        glex.setGlobalValue("ODEs", astBodyDecorator.getOdeDefinition().get().getODEs());
      }

    }

  }

  private static String convertToCppNamespaceConvention(String fqnName) {
    return fqnName.replace(".", "::");
  }


}
