package com.rinko.incenseterminal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinko.incenseterminal.core.engine.WorkloadViewModel
import com.rinko.incenseterminal.data.WorkloadRow
import com.rinko.incenseterminal.ui.theme.IncenseColors

@Composable
fun WorkloadScreen(
    onSelectDone: () -> Unit,
    viewModel: WorkloadViewModel = viewModel()
) {
    val workloads by viewModel.workloads.collectAsState()
    val currentWl by viewModel.currentWorkload.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDuration by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$ workload",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = IncenseColors.Success
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(workloads) { wl ->
                WorkloadRow(
                    workload = wl,
                    isSelected = currentWl?.id == wl.id,
                    onSelect = {
                        if (currentWl?.id != wl.id) {
                            viewModel.selectWorkload(wl)
                            onSelectDone()
                        }
                    },
                    onDelete = { viewModel.deleteWorkload(wl) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (showAdd) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                BasicTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.width(120.dp),
                    textStyle = TextStyle(
                        color = IncenseColors.PrimaryText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(IncenseColors.Accent)
                )
                Text(
                    text = "  ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = IncenseColors.DimText
                )
                BasicTextField(
                    value = newDuration,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) newDuration = it },
                    modifier = Modifier.width(50.dp),
                    textStyle = TextStyle(
                        color = IncenseColors.PrimaryText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(IncenseColors.Accent)
                )
                Text(
                    text = "m",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = IncenseColors.DimText
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "[ add ]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = IncenseColors.Success,
                    modifier = Modifier.clickable {
                        if (newName.isNotBlank() && newDuration.isNotBlank()) {
                            viewModel.addWorkload(newName.trim(), newDuration.toIntOrNull() ?: 25)
                            newName = ""
                            newDuration = ""
                            showAdd = false
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Text(
                text = if (showAdd) "[ cancel ]" else "[ + new ]",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = IncenseColors.Accent,
                modifier = Modifier.clickable {
                    if (showAdd) {
                        showAdd = false
                        newName = ""
                        newDuration = ""
                    } else {
                        showAdd = true
                    }
                }.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun WorkloadRow(
    workload: WorkloadRow,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isSelected) ">" else " ",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = if (isSelected) IncenseColors.Ember else IncenseColors.DimText
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = workload.name.replace("_", " "),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = if (isSelected) IncenseColors.PrimaryText else IncenseColors.DimText,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${workload.defaultDurationMinutes}m",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = IncenseColors.DimText
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "x",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = IncenseColors.Warning,
            modifier = Modifier.clickable(onClick = onDelete).padding(horizontal = 4.dp)
        )
    }
}
