package cn.dong.charcount

import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import kotlin.math.ceil

class DebugLengthFilter(max: Int) : InputFilter.LengthFilter(max) {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val filterSource = super.filter(source, start, end, dest, dstart, dend)
        Log.d(
            "DebugLengthFilter", "filter: source=$source, start=$start, end=$end, " +
                    "dest=$dest, dstart=$dstart, dend=$dend, filterSource=[$filterSource]"
        )
        return filterSource
    }
}

/**
 * 每个英文字符算 0.5 个字数，每个中文字符算 1 个字数。这是为了相同字数限制能输入更多英文，因为表达相同含义需要更多英文字母。
 * 实际算的 UTF-16 数量，这个方案各端容易统一，也是业界普遍使用的方案，比如 Google 翻译、Witter、微博都是此方案。
 * 但 Emoji 字符一般会包含 2 个或更多个 UTF-16（比如 Modifier sequences 和 ZWJ sequences），导致一个 Emoji 会占更多字数，上述产品也是如此。
 */
fun CharSequence.charCount(): Double = sumByDouble { it.count() }

/** [charCount] 结果向上取整 */
fun CharSequence.charCountCeil(): Int = ceil(charCount()).toInt()

private fun Char.count(): Double {
    return if (toInt() < 128) {
        0.5 // ASCII
    } else {
        1.0
    }
}

/** 按字数截取，每个英文字符算 0.5 个字数，每个中文字符算 1 个字数。 */
fun CharSequence.trimByCharCount(charCount: Int): CharSequence {
    var sum = 0.0
    for (index in 0 until length) {
        val char = get(index)
        sum += char.count()
        if (sum > charCount) {
            var end = index
            if (index == 0) {
                return ""
            } else if (get(index - 1).isHighSurrogate()) {
                end-- // 如果前一个字符是高位，需要再向前截取一位
            }
            return subSequence(0, end)
        }
    }
    return this
}

/**
 * 字数限制。每个英文字符算 0.5 个字数，每个中文字符算 1 个字数。
 * 详细字数计算规则参考 [CharSequence.charCount]，两者一致。
 */
class CharCountFilter(private val max: Int) : InputFilter {

    /**
     * 用 source 的 [start, end) 区间，替换掉 dest 中的 [dstart, dend) 区间。
     * dstart = dend 时表示不替换原有字符。dstart < dend 时表示替换区间 [dstart, dend) 的字符，比如光标选中了部分，或者自动补全。
     * @return 最终替换的字符串，return null 表示不改变输入，return "" 表示拒绝输入。
     */
    override fun filter(
        source: CharSequence, // 新输入的文字
        start: Int, // 开始位置（inclusive）
        end: Int, // 结束位置（exclusive）
        dest: Spanned, // 当前显示的内容
        dstart: Int, // 输入目标开始位置
        dend: Int // 输入目标结束位置
    ): CharSequence? {
        var destCharCount = 0.0 // 当前字符被替换后的数量
        for ((index, char) in dest.withIndex()) {
            if (index < dstart || index >= dend) {
                // 迭代文字剩余部分，剔除了被替换的区间
                destCharCount += char.count()
            }
        }
        val keep = max - destCharCount
        if (keep <= 0) {
            return "" // 禁止输入
        } else {
            var sourceCharCount = 0.0
            for (index in start until end) {
                val char = source[index]
                sourceCharCount += char.count()
                if (sourceCharCount > keep) {
                    if (index == start) {
                        // 第一个字符就超过限制了，拒绝输入
                        return ""
                    } else {
                        // 当前字符超了，从前一个字符截取
                        var sourceEnd = index // (exclusive)
                        if (source[sourceEnd - 1].isHighSurrogate()) {
                            sourceEnd-- // 如果前一个字符是高位，需要再向前截取一位
                            if (sourceEnd == start) {
                                return ""
                            }
                        }
                        return source.subSequence(start, sourceEnd)
                    }
                }
            }
            return null // keep original
        }
    }
}
