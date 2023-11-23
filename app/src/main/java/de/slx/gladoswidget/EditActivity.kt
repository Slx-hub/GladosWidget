package de.slx.gladoswidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview

class EditActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			val text = remember { mutableStateOf("") }

			Column {
				TextField(
					value = text.value,
					onValueChange = { text.value = it },
					label = { Text("Enter text") }
				)

				Button(onClick = {
					// Save the text somewhere, e.g., shared preferences or a database
				}) {
					Text("Save")
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
	EditActivity()
}