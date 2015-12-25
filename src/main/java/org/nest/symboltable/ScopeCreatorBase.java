/*
 * Copyright (c)  RWTH Aachen. All rights reserved.
 *
 * http://www.se-rwth.de/
 */
package org.nest.symboltable;

import de.monticore.symboltable.GlobalScope;
import de.se_rwth.commons.logging.Log;
import org.nest.symboltable.predefined.PredefinedFunctionFactory;
import org.nest.symboltable.predefined.PredefinedTypes;
import org.nest.symboltable.predefined.PredefinedVariablesFactory;

/**
 * TODO
 *
 * @author plotnikov
 */
public abstract class ScopeCreatorBase {

  protected final PredefinedFunctionFactory functionFactory;
  protected final PredefinedVariablesFactory variablesFactory;

  public abstract String getLogger();

  public ScopeCreatorBase() {
    this.functionFactory = new PredefinedFunctionFactory();
    this.variablesFactory = new PredefinedVariablesFactory();
  }


  public void addPredefinedTypes(final GlobalScope globalScope) {
    PredefinedTypes.getTypes().forEach(
        type -> {
          globalScope.add(type);
          final String typeLogMsg = "Adds new implicit type declaration: %s";
          Log.info(String.format(typeLogMsg, type.getName()), getLogger());
        }
    );
  }

  public void addPredefinedFunctions(final GlobalScope globalScope) {

    functionFactory.getMethodSymbols().forEach(
        method -> {
          globalScope.add(method);
          final String methodLogMsg = String
              .format("Adds new implicit method declaration: %s", method.getName());
          Log.info(methodLogMsg, getLogger());
        }
    );
  }

  public void addPredefinedVariables(final GlobalScope globalScope) {

    variablesFactory.gerVariables().forEach(
        variable -> {
          globalScope.add(variable);
          final String methodLogMsg = String
              .format("Adds new implicit variable declaration: %s", variable.getName());
          Log.info(methodLogMsg, getLogger());
        }
    );

  }

}
