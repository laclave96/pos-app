package com.savent.erp.utils

class NameFormat {

    companion object {
        fun format(name: String): String{
            val strBuilder = StringBuilder()
            strBuilder.append(name[0].uppercaseChar())
            strBuilder.append(name[1].lowercaseChar())
            for (i in 2 until name.length){
                if(name[i-1].isWhitespace()) strBuilder.append(name[i].uppercaseChar())
                else strBuilder.append(name[i].lowercaseChar())
            }
            return strBuilder.toString()
        }
    }
}