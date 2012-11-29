# -*- coding: utf-8 -*-
class Helper(object):
    @staticmethod
    def translit(str):
        table = {u'а': u'a', u'б': u'b', u'в': u'v', u'г': u'g', u'д': u'd', u'е': u'e', u'ё': u'e',
                 u'ж': u'zh', u'з': u'z', u'и': u'i', u'й': u'i', u'к': u'k', u'л': u'l', u'м': u'm', u'н': u'n',
                 u'о': u'o', u'п': u'p', u'р': u'r', u'с': u's', u'т': u't', u'у': u'u', u'ф': u'f', u'х': u'h',
                 u'ц': u'c', u'ч': u'cz', u'ш': u'sh', u'щ': u'scz', u'ъ': u'', u'ы': u'y', u'ь': u'', u'э': u'e',
                 u'ю': u'u', u'я': u'ya', u'А': u'a', u'Б': u'b', u'В': u'v', u'Г': u'g', u'Д': u'd', u'Е': u'e',
                 u'Ё': u'e', u'Ж': u'zh', u'З': u'z', u'И': u'i', u'Й': u'i', u'К': u'k', u'Л': u'l', u'М': u'm',
                 u'Н': u'n', u'О': u'o', u'П': u'p', u'Р': u'r', u'С': u's', u'Т': u't', u'У': u'u', u'Ф': u'Х',
                 u'х': u'h', u'Ц': u'c', u'Ч': u'cz', u'Ш': u'sh', u'Щ': u'scz', u'Ъ': u'', u'Ы': u'y', u'Ь': u'',
                 u'Э': u'e', u'Ю': u'u', u'Я': u'ya', u',': u'', u'?': u'', u' ': u'_', u'~': u'', u'!': u'',
                 u'@': u'', u'#': u'', u'$': u'', u'%': u'', u'^': u'', u'&': u'', u'*': u'', u'(': u'', u')': u'',
                 u'-': u'', u'=': u'', u'+': u'', u':': u'', u';': u'', u'<': u'', u'>': u'', u'\'': u'', u'"': u'',
                 u'\\': u'', u'/': u'', u'№': u'', u'[': u'', u']': u'', u'{': u'', u'}': u'', u'ґ': u'', u'ї': u'',
                 u'є': u'', u'Ґ': u'g', u'Ї': u'i', u'Є': u'e'}

        for key in table:
            str = str.replace(key, table[key])
        return str

