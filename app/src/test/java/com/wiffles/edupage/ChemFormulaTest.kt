package com.wiffles.edupage

import com.wiffles.edupage.ui.chat.chemFormula
import com.wiffles.edupage.ui.chat.normalizeChem
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Chemistry typesetting for the chat `edupage-chem` block: digits after an element become
 * subscripts, `_`/`^` scripts (and charges) are converted, and leading coefficients stay
 * upright. Guards against reactions regressing into overflowing `$...$` maths.
 */
class ChemFormulaTest {

    @Test
    fun elementCountsBecomeSubscripts() {
        assertEquals("H₂O", chemFormula("H2O").text)
        assertEquals("C₆H₁₂O₆", chemFormula("C6H12O6").text)
    }

    @Test
    fun groupCountsAndLeadingCoefficientsStayUpright() {
        assertEquals("2 H₂O", chemFormula("2 H2O").text)
        assertEquals("Ca(OH)₂", chemFormula("Ca(OH)2").text)
    }

    @Test
    fun chargesAndExplicitScriptsAreConverted() {
        assertEquals("SO₄²⁻", chemFormula("SO4^2-").text)
        assertEquals("SO₄²⁻", chemFormula("SO_4^{2-}").text)
    }

    @Test
    fun latexWrappersAndCommandsAreStripped() {
        assertEquals(
            "2 H₂ + O₂ → 2 H₂O",
            chemFormula("\\text{2 H_2 + O_2} \\rightarrow \\text{2 H_2O}").text,
        )
        assertEquals("2 H2 + O2 → 2 H2O", normalizeChem("\\text{2 H2 + O2} \\rightarrow \\text{2 H2O}"))
        assertEquals("H₂O", chemFormula("\\mathrm{H_2O}").text)
    }
}