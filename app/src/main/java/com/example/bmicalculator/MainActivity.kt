package com.example.bmicalculator

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bmicalculator.databinding.ActivityMainBinding
import android.view.View
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUnitSpinners()

        binding.calculateBtn.setOnClickListener {
            calculateBMI()
        }
    }

    private fun setupUnitSpinners() {
        val weightAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.weight_units,
            android.R.layout.simple_spinner_item
        )
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.weightUnitSpinner.adapter = weightAdapter

        val heightAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.height_units,
            android.R.layout.simple_spinner_item
        )
        heightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.heightUnitSpinner.adapter = heightAdapter

        binding.weightUnitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.weightEdit.hint = parent?.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.heightUnitSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.heightEdit.hint = parent?.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun calculateBMI() {
        val weightValue = binding.weightEdit.text.toString().toFloatOrNull()
        val heightValue = binding.heightEdit.text.toString().toFloatOrNull()

        if (weightValue != null && heightValue != null) {
            val weightInKg = when (binding.weightUnitSpinner.selectedItem.toString()) {
                "lb" -> weightValue * 0.453592f
                else -> weightValue
            }

            val heightInMeters = when (binding.heightUnitSpinner.selectedItem.toString()) {
                "cm" -> heightValue / 100f
                "in" -> heightValue * 0.0254f
                else -> heightValue
            }

            if (heightInMeters > 0) {
                val bmi = weightInKg / heightInMeters.pow(2)
                val bmiResult = String.format("%.2f", bmi)

                val (bmiCategory, colorRes, healthTip) = when {
                    bmi < 18.5 -> Triple(
                        "Underweight",
                        R.color.bmi_underweight,
                        "Focus on nutrient-rich foods and building muscle mass through strength training."
                    )
                    bmi < 25 -> Triple(
                        "Normal weight",
                        R.color.bmi_normal,
                        "Great job! Maintain a balanced diet and regular physical activity."
                    )
                    bmi < 30 -> Triple(
                        "Overweight",
                        R.color.bmi_overweight,
                        "Try incorporating more whole grains, fruits, and vegetables into your diet."
                    )
                    else -> Triple(
                        "Obese",
                        R.color.bmi_obese,
                        "Consult a healthcare professional for a tailored plan to improve your health."
                    )
                }

                binding.resultCard.visibility = View.VISIBLE
                binding.resultCard.setCardBackgroundColor(ContextCompat.getColor(this, colorRes))
                binding.resultText.text = "BMI: $bmiResult"
                binding.categoryText.text = bmiCategory
                binding.tipText.text = healthTip
            } else {
                binding.resultCard.visibility = View.GONE
            }
        } else {
            binding.resultCard.visibility = View.GONE
        }
    }
}