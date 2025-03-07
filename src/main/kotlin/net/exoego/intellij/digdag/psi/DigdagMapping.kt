package net.exoego.intellij.digdag.psi

interface DigdagMapping : DigdagCompoundValue {
    fun getKeyValues(): Collection<DigdagKeyValue>

    fun getKeyValueByKey(keyText: String): DigdagKeyValue?

    fun getChildMapping(keyText: String): DigdagMapping?

    fun getChildSequence(keyText: String): DigdagSequence?

    fun putKeyValue(keyValueToAdd: DigdagKeyValue)

    /**
     * This one's different from plain deletion in a way that excess newlines/commas are also deleted
     */
    fun deleteKeyValue(keyValueToDelete: DigdagKeyValue)
}