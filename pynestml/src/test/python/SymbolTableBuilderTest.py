#
# SymbolTableBuilderTest.py
#
# This file is part of NEST.
#
# Copyright (C) 2004 The NEST Initiative
#
# NEST is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 2 of the License, or
# (at your option) any later version.
#
# NEST is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with NEST.  If not, see <http://www.gnu.org/licenses/>.

from __future__ import print_function

import unittest
import os
from antlr4 import *
from pynestml.src.main.grammars.org.PyNESTMLParser import PyNESTMLParser
from pynestml.src.main.grammars.org.PyNESTMLLexer import PyNESTMLLexer
from pynestml.src.main.python.org.nestml.visitor.ASTSymbolTableVisitor import SymbolTableASTVisitor
from pynestml.src.main.python.org.nestml.visitor.ASTBuilderVisitor import ASTBuilderVisitor


class SymbolTableBuilderTest(unittest.TestCase):
    def test(self):
        for filename in os.listdir(os.path.realpath(os.path.join(os.path.dirname(__file__),
                                                                 os.path.join('..', '..', '..', '..', 'models')))):
            if filename.endswith(".nestml"):
                inputFile = FileStream(
                    os.path.join(os.path.dirname(__file__), os.path.join(os.path.join('..', '..', '..', '..',
                                                                                      'models'), filename)))
                lexer = PyNESTMLLexer(inputFile)
                # create a token stream
                stream = CommonTokenStream(lexer)
                # parse the file
                parser = PyNESTMLParser(stream)
                # create a new visitor and return the new AST
                astBuilderVisitor = ASTBuilderVisitor()
                ast = astBuilderVisitor.visit(parser.nestmlCompilationUnit())
                # update the corresponding symbol tables
                for neuron in ast.getNeuronList():
                    SymbolTableASTVisitor.updateSymbolTable(neuron)
        return


if __name__ == '__main__':
    unittest.main()
