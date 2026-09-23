package com.example.bmicalculator

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import com.example.bmicalculator.databinding.ActivityMainBinding
import com.google.android.material.color.MaterialColors
import com.google.android.material.transition.MaterialFadeThrough
import androidx.transition.TransitionManager
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private data class BmiCategory(
        val labelRes: Int,
        val tipRes: Int,
        val colorRes: Int,
        val containerColorRes: Int,
        val onContainerColorRes: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setupUnitToggles()
        styleGauge()

        binding.weightEdit.doAfterTextChanged { binding.weightInputLayout.clearError() }
        binding.heightEdit.doAfterTextChanged { binding.heightInputLayout.clearError() }

        binding.calculateBtn.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            hideKeyboard()
            view.clearFocus()
            calculateBmi()
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.header) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top + HEADER_TOP_PADDING_DP.dp)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentScroll) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            view.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    private fun setupUnitToggles() {
        binding.weightUnitToggle.check(R.id.unitKg)
        binding.heightUnitToggle.check(R.id.unitCm)

        binding.weightUnitToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            binding.weightInputLayout.suffixText = weightUnit(checkedId)
            binding.weightInputLayout.clearError()
        }
        binding.heightUnitToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            binding.heightInputLayout.suffixText = heightUnit(checkedId)
            binding.heightInputLayout.clearError()
        }
    }

    private fun styleGauge() {
        binding.bmiGauge.labelColor =
            MaterialColors.getColor(binding.bmiGauge, com.google.android.material.R.attr.colorOnSurfaceVariant)
        binding.bmiGauge.markerOutlineColor =
            MaterialColors.getColor(binding.bmiGauge, com.google.android.material.R.attr.colorSurface)
    }

    private fun weightUnit(checkedId: Int): String =
        if (checkedId == R.id.unitLb) getString(R.string.unit_lb) else getString(R.string.unit_kg)

    private fun heightUnit(checkedId: Int): String = when (checkedId) {
        R.id.unitM -> getString(R.string.unit_m)
        R.id.unitIn -> getString(R.string.unit_in)
        else -> getString(R.string.unit_cm)
    }

    private fun calculateBmi() {
        val weightValue = binding.weightEdit.text.toString().toFloatOrNull()
        val heightValue = binding.heightEdit.text.toString().toFloatOrNull()

        val weightInKg = weightValue?.let {
            when (binding.weightUnitToggle.checkedButtonId) {
                R.id.unitLb -> it * LB_TO_KG
                else -> it
            }
        }
        val heightInMeters = heightValue?.let {
            when (binding.heightUnitToggle.checkedButtonId) {
                R.id.unitM -> it
                R.id.unitIn -> it * IN_TO_M
                else -> it / 100f
            }
        }

        val weightValid = validate(
            binding.weightInputLayout,
            weightInKg,
            MIN_WEIGHT_KG..MAX_WEIGHT_KG,
            R.string.error_weight_required,
            R.string.error_weight_range
        )
        val heightValid = validate(
            binding.heightInputLayout,
            heightInMeters,
            MIN_HEIGHT_M..MAX_HEIGHT_M,
            R.string.error_height_required,
            R.string.error_height_range
        )
        if (!weightValid || !heightValid || weightInKg == null || heightInMeters == null) {
            hideResult()
            return
        }

        showResult(weightInKg / heightInMeters.pow(2))
    }

    private fun validate(
        layout: TextInputLayout,
        value: Float?,
        validRange: ClosedFloatingPointRange<Float>,
        requiredError: Int,
        rangeError: Int
    ): Boolean = when {
        value == null -> {
            layout.error = getString(requiredError)
            false
        }
        value !in validRange -> {
            layout.error = getString(rangeError)
            false
        }
        else -> {
            layout.clearError()
            true
        }
    }

    private fun showResult(bmi: Float) {
        val category = categoryFor(bmi)
        val wasVisible = binding.resultCard.isVisible()

        TransitionManager.beginDelayedTransition(
            binding.resultCard.parent as android.view.ViewGroup,
            MaterialFadeThrough()
        )
        binding.resultCard.visibility = View.VISIBLE

        binding.categoryChip.text = getString(category.labelRes)
        binding.categoryChip.chipBackgroundColor =
            ContextCompat.getColorStateList(this, category.containerColorRes)
        binding.categoryChip.setTextColor(ContextCompat.getColor(this, category.onContainerColorRes))
        binding.resultText.setTextColor(ContextCompat.getColor(this, category.colorRes))
        binding.tipText.text = getString(category.tipRes)
        binding.bmiGauge.setBmi(bmi)

        animateBmiValue(from = if (wasVisible) currentDisplayedBmi() else 0f, to = bmi)

        if (!wasVisible) {
            binding.resultCard.alpha = 0f
            binding.resultCard.translationY = RESULT_SLIDE_DP.dp.toFloat()
            binding.resultCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(RESULT_ANIMATION_MS)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        binding.contentScroll.post {
            binding.contentScroll.smoothScrollTo(0, binding.resultCard.top)
        }
    }

    private fun animateBmiValue(from: Float, to: Float) {
        ValueAnimator.ofFloat(from, to).apply {
            duration = RESULT_ANIMATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                binding.resultText.text = formatBmi(it.animatedValue as Float)
            }
            start()
        }
    }

    private fun currentDisplayedBmi(): Float =
        binding.resultText.text.toString().toFloatOrNull() ?: 0f

    private fun hideResult() {
        if (!binding.resultCard.isVisible()) return
        TransitionManager.beginDelayedTransition(
            binding.resultCard.parent as android.view.ViewGroup,
            MaterialFadeThrough()
        )
        binding.resultCard.visibility = View.GONE
    }

    private fun categoryFor(bmi: Float): BmiCategory = when {
        bmi < 18.5f -> BmiCategory(
            R.string.category_underweight,
            R.string.tip_underweight,
            R.color.bmi_underweight,
            R.color.bmi_underweight_container,
            R.color.bmi_underweight_on_container
        )
        bmi < 25f -> BmiCategory(
            R.string.category_normal,
            R.string.tip_normal,
            R.color.bmi_normal,
            R.color.bmi_normal_container,
            R.color.bmi_normal_on_container
        )
        bmi < 30f -> BmiCategory(
            R.string.category_overweight,
            R.string.tip_overweight,
            R.color.bmi_overweight,
            R.color.bmi_overweight_container,
            R.color.bmi_overweight_on_container
        )
        else -> BmiCategory(
            R.string.category_obese,
            R.string.tip_obese,
            R.color.bmi_obese,
            R.color.bmi_obese_container,
            R.color.bmi_obese_on_container
        )
    }

    private fun formatBmi(value: Float): String =
        String.format(Locale.getDefault(), "%.1f", value)

    private fun TextInputLayout.clearError() {
        if (error != null) error = null
    }

    private fun View.isVisible(): Boolean = visibility == View.VISIBLE

    private fun hideKeyboard() {
        WindowCompat.getInsetsController(window, binding.root)
            .hide(WindowInsetsCompat.Type.ime())
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    companion object {
        private const val LB_TO_KG = 0.453592f
        private const val IN_TO_M = 0.0254f
        private const val MIN_WEIGHT_KG = 2f
        private const val MAX_WEIGHT_KG = 500f
        private const val MIN_HEIGHT_M = 0.5f
        private const val MAX_HEIGHT_M = 2.7f
        private const val HEADER_TOP_PADDING_DP = 24
        private const val RESULT_SLIDE_DP = 24
        private const val RESULT_ANIMATION_MS = 500L
    }
}
