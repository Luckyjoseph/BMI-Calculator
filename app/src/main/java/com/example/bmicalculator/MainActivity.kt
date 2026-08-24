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

                val (bmiCategory, colorRes) = when {
                    bmi < 18.5 -> "Underweight" to R.color.bmi_underweight
                    bmi < 25 -> "Normal weight" to R.color.bmi_normal
                    bmi < 30 -> "Overweight" to R.color.bmi_overweight
                    else -> "Obese" to R.color.bmi_obese
                }

                binding.resultCard.visibility = View.VISIBLE
                binding.resultCard.setCardBackgroundColor(ContextCompat.getColor(this, colorRes))
                binding.resultText.text = "BMI: $bmiResult"
                binding.categoryText.text = bmiCategory
            } else {
                binding.resultCard.visibility = View.GONE
            }
        } else {
            binding.resultCard.visibility = View.GONE
        }
    }
}