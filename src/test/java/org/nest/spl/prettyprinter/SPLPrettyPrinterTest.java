package org.nest.spl.prettyprinter;import org.junit.Test;import org.nest.spl._ast.ASTSPLFile;import org.nest.spl._parser.SPLParser;import org.nest.spl.symboltable.SPLScopeCreator;import org.nest.symboltable.predefined.PredefinedTypes;import java.io.File;import java.io.IOException;import java.io.StringReader;import java.util.Optional;import static org.junit.Assert.assertTrue;/** * Checks that the pretty printed result can be parsed again. A comparison on the AST level could be implemented in * the future. * @author (last commit) $Author$ * @version $Revision$, $Date$ */public class SPLPrettyPrinterTest {  private static final String TEST_MODEL_PATH = "src/test/resources/";  private final SPLParser splFileParser = new SPLParser();  private final ExpressionsPrettyPrinter prettyPrinter = new ExpressionsPrettyPrinter();  private final SPLScopeCreator splScopeCreator = new SPLScopeCreator(TEST_MODEL_PATH);  private Optional<ASTSPLFile> parseStringAsSPLFile(final String fileAsString) throws IOException {    return splFileParser.parse(new StringReader(fileAsString));  }  @Test  public void testThatPrettyPrinterProducesParsableOutput() throws IOException {    final SPLPrettyPrinter splPrettyPrinter = new SPLPrettyPrinter(prettyPrinter);    final Optional<ASTSPLFile> root = splFileParser.parse        ("src/test/resources/org/nest/spl/parsing/modelContainingAllLanguageElements.simple");    assertTrue(root.isPresent());    // TODO write frontend manager for the cocos and check them on the model    splScopeCreator.runSymbolTableCreator(root.get());    root.get().accept(splPrettyPrinter); // starts prettyPrinter    System.out.println(splPrettyPrinter.getResult());    Optional<ASTSPLFile> prettyPrintedRoot = parseStringAsSPLFile(splPrettyPrinter.getResult());    assertTrue(prettyPrintedRoot.isPresent());  }  @Test  public void testAllModelsForParser() throws IOException {    parseAllSPLModelsFromFolder("src/test/resources/org/nest/spl/parsing");  }  @Test  public void testAllModelsForCocos() throws IOException {    parseAllSPLModelsFromFolder("src/test/resources/org/nest/spl/cocos");  }  private void parseAllSPLModelsFromFolder(final String folderPath) throws IOException {    final File parserModelsFolder = new File(folderPath);    for (File splModelFile : parserModelsFolder.listFiles()) {      System.out.println("Handles the model: " + splModelFile.getPath());      final SPLPrettyPrinter splPrettyPrinter = new SPLPrettyPrinter(prettyPrinter);      final Optional<ASTSPLFile> splModelRoot = splFileParser.parse(splModelFile.getPath());      assertTrue("Cannot parse the model: " + splModelFile.getName(), splModelRoot.isPresent());      splScopeCreator.runSymbolTableCreator(splModelRoot.get());      splModelRoot.get().accept(splPrettyPrinter);      System.out.println(splPrettyPrinter.getResult());      Optional<ASTSPLFile> prettyPrintedRoot = parseStringAsSPLFile(splPrettyPrinter.getResult());      assertTrue(prettyPrintedRoot.isPresent());    }  }}