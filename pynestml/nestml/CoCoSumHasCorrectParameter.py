#
# CoCoSumHasCorrectParameter.py
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
from pynestml.nestml.CoCo import CoCo
from pynestml.nestml.ASTNeuron import ASTNeuron
from pynestml.nestml.NESTMLVisitor import NESTMLVisitor
from pynestml.nestml.ASTSimpleExpression import ASTSimpleExpression
from pynestml.utils.Logger import LOGGING_LEVEL, Logger
from pynestml.utils.Messages import Messages


class CoCoSumHasCorrectParameter(CoCo):
    """
    This coco ensures that cur_sum,cond_sum and convolve get only simple variable references as inputs.
    Not allowed:
     V mV = convolve(g_in+g_ex,Buffer)
    """

    @classmethod
    def checkCoCo(cls, _neuron=None):
        """
        Ensures the coco for the handed over neuron.
        :param _neuron: a single neuron instance.
        :type _neuron: ASTNeuron
        """
        assert (_neuron is not None and isinstance(_neuron, ASTNeuron)), \
            '(PyNestML.CoCo.BufferNotAssigned) No or wrong type of neuron provided (%s)!' % type(_neuron)
        cls.neuronName = _neuron.getName()
        visitor = SumIsCorrectVisitor()
        _neuron.accept(visitor)
        return


class SumIsCorrectVisitor(NESTMLVisitor):
    """
    This visitor ensures that sums/convolve are provided with a correct expression.
    """

    def visitFunctionCall(self, _functionCall=None):
        """
        Checks the coco on the current function call.
        :param _functionCall: a single function call.
        :type _functionCall: ASTFunctionCall
        """
        from pynestml.nestml.ASTFunctionCall import ASTFunctionCall
        assert (_functionCall is not None and isinstance(_functionCall, ASTFunctionCall)), \
            '(PyNestML.CoCo.SumHasCorrectParameter) No or wrong type of function call provided (%s)!' % type(
                _functionCall)
        fName = _functionCall.getName()
        if fName == 'cur_sum' or fName == 'cond_sum' or fName == 'convolve':
            for arg in _functionCall.getArgs():
                if not isinstance(arg, ASTSimpleExpression) or not arg.isVariable():
                    code, message = Messages.getNotAVariable(arg.printAST())
                    Logger.logMessage(_code=code, _message=message,
                                      _errorPosition=arg.getSourcePosition(), _logLevel=LOGGING_LEVEL.ERROR)
        return
