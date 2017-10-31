#
# NESTML2NESTTypeConverter.py
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
from pynestml.nestml.PredefinedTypes import PredefinedTypes
from pynestml.nestml.TypeSymbol import TypeSymbol


class NESTML2NESTTypeConverter(object):
    """
    This class contains a single operation as used to convert nestml types to nest centerpieces.
    """

    @classmethod
    def convert(cls, _typeSymbol=None):
        """
        Converts the name of the type symbol to a corresponding nest representation.
        :param _typeSymbol: a single type symbol
        :type _typeSymbol: TypeSymbol
        :return: the corresponding string representation.
        :rtype: str
        """
        assert (_typeSymbol is not None and isinstance(_typeSymbol, TypeSymbol)), \
            '(PyNestML.CodeGeneration.TypeConverter) No or wrong type of type symbol provided (%s)!' % type(_typeSymbol)
        if _typeSymbol.equals(PredefinedTypes.getStringType()):
            return 'std::string'
        if _typeSymbol.equals(PredefinedTypes.getVoidType()):
            return 'void'
        if _typeSymbol.isBuffer():
            return 'nest::RingBuffer'
        if _typeSymbol.equals(PredefinedTypes.getBooleanType()):
            return 'bool'
        if _typeSymbol.equals(PredefinedTypes.getRealType()):
            return 'double'
        if _typeSymbol.isUnit():
            return 'double'
        if _typeSymbol.equals(PredefinedTypes.getIntegerType()):
            return 'long'
        if 'Time' in _typeSymbol.getSymbolName():
            return 'nest::Time'
        return _typeSymbol.getSymbolName().replace('.', '::')

