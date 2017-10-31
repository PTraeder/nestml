#
# LineOperatorVisitor.py
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

"""
expression : left=expression (plusOp='+'  | minusOp='-') right=expression
"""
from pynestml.nestml.ASTArithmeticOperator import ASTArithmeticOperator
from pynestml.nestml.PredefinedTypes import PredefinedTypes
from pynestml.nestml.ErrorStrings import ErrorStrings
from pynestml.nestml.NESTMLVisitor import NESTMLVisitor
from pynestml.nestml.Either import Either
from pynestml.utils.Logger import Logger, LOGGING_LEVEL
from pynestml.utils.Messages import MessageCode


class LineOperatorVisitor(NESTMLVisitor):
    """
    Visits a single binary operation consisting of + or - and updates the type accordingly.
    """

    def visitExpression(self, _expr=None):
        """
        TODO comments
        :param _expr:
        :type _expr:
        :return:
        :rtype:
        """
        lhsTypeE = _expr.getLhs().getTypeEither()
        rhsTypeE = _expr.getRhs().getTypeEither()

        if lhsTypeE.isError():
            _expr.setTypeEither(lhsTypeE)
            return
        if rhsTypeE.isError():
            _expr.setTypeEither(rhsTypeE)
            return

        lhsType = lhsTypeE.getValue()
        rhsType = rhsTypeE.getValue()

        arithOp = _expr.getBinaryOperator()
        # arithOp exists if we get into this visitor, but make sure:
        assert arithOp is not None and isinstance(arithOp, ASTArithmeticOperator)

        # Plus-exclusive code
        if arithOp.isPlusOp():
            # String concatenation has a prio. If one of the operands is a string,
            # the remaining sub-expression becomes a string
            if (lhsType.isString() or rhsType.isString()) and (not rhsType.isVoid() and not lhsType.isVoid()):
                _expr.setTypeEither(Either.value(PredefinedTypes.getStringType()))
                return

        # Common code for plus and minus ops:
        if lhsType.isNumeric() and rhsType.isNumeric():
            # both match exactly -> any is valid
            if lhsType.equals(rhsType):
                _expr.setTypeEither(Either.value(lhsType))
                return
            # both numeric primitive, not matching -> one is real one is integer -> real
            if lhsType.isNumericPrimitive() and rhsType.isNumericPrimitive():
                _expr.setTypeEither(Either.value(PredefinedTypes.getRealType()))
                return
            # Both are units, not matching -> real, WARN
            if lhsType.isUnit() and rhsType.isUnit():
                errorMsg = ErrorStrings.messageAddSubTypeMismatch \
                    (self, lhsType.printSymbol(), rhsType.printSymbol(), "real", _expr.getSourcePosition())
                _expr.setTypeEither(Either.value(PredefinedTypes.getRealType()))
                Logger.logMessage(_code=MessageCode.ADD_SUB_TYPE_MISMATCH,
                                  _errorPosition=_expr.getSourcePosition(),
                                  _message=errorMsg, _logLevel=LOGGING_LEVEL.WARNING)
                return
            # one is unit and one numeric primitive and vice versa -> assume unit, WARN
            if (lhsType.isUnit() and rhsType.isNumericPrimitive()) or (
                        rhsType.isUnit() and lhsType.isNumericPrimitive()):
                unitType = None
                if lhsType.isUnit():
                    unitType = lhsType
                else:
                    unitType = rhsType
                errorMsg = ErrorStrings.messageAddSubTypeMismatch \
                    (self, lhsType.printSymbol(), rhsType.printSymbol(), unitType.printSymbol(),
                     _expr.getSourcePosition())
                _expr.setTypeEither(Either.value(unitType))
                Logger.logMessage(_code=MessageCode.ADD_SUB_TYPE_MISMATCH, _message=errorMsg,
                                  _errorPosition=_expr.getSourcePosition(), _logLevel=LOGGING_LEVEL.WARNING)
                return

        # if we get here, we are in a general error state
        errorMsg = ErrorStrings.messageAddSubTypeMismatch \
            (self, lhsType.printSymbol(), rhsType.printSymbol(), "ERROR", _expr.getSourcePosition())
        _expr.setTypeEither(Either.error(errorMsg))
        Logger.logMessage(_code=MessageCode.ADD_SUB_TYPE_MISMATCH, _message=errorMsg,
                          _errorPosition=_expr.getSourcePosition(), _logLevel=LOGGING_LEVEL.ERROR)
