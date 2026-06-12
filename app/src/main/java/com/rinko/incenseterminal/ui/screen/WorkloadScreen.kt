package com.rinko.incenseterminal.ui.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinko.incenseterminal.core.engine.WorkloadViewModel
import com.rinko.incenseterminal.data.WorkloadRow
import com.rinko.incenseterminal.ui.theme.IncenseColors
import com.rinko.incenseterminal.ui.theme.MonospaceFamily

@Composable
fun WorkloadScreen(
    onSelectDone: () -> Unit,
    viewModel: WorkloadViewModel = viewModel()
) {
    val workloads by viewModel.workloads.collectAsState()
    val currentWl by viewModel.currentWorkload.collectAsState()
    val todayCounts by viewModel.todayCounts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingWorkload by remember { mutableStateOf<WorkloadRow?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$ Workload",
                fontFamily = MonospaceFamily,
                fontSize = 14.sp,
                color = IncenseColors.Success
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "[ + new ]",
                fontFamily = MonospaceFamily,
                fontSize = 14.sp,
                color = IncenseColors.Accent,
                modifier = Modifier
                    .clickable { showAddDialog = true }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TableHeader()

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(workloads) { wl ->
                WorkloadTableRow(
                    workload = wl,
                    isSelected = currentWl?.id == wl.id,
                    todayCount = todayCounts[wl.name] ?: 0,
                    onSelect = {
                        if (currentWl?.id != wl.id) {
                            viewModel.selectWorkload(wl)
                            onSelectDone()
                        }
                    },
                    onEdit = { editingWorkload = wl }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showAddDialog) {
        AddWorkloadDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, dur ->
                viewModel.addWorkload(name, dur)
                showAddDialog = false
            }
        )
    }

    editingWorkload?.let { wl ->
        EditWorkloadDialog(
            workload = wl,
            onDismiss = { editingWorkload = null },
            onSave = { name, dur ->
                viewModel.updateWorkload(wl, name, dur)
                editingWorkload = null
            },
            onDelete = {
                viewModel.deleteWorkload(wl)
                editingWorkload = null
            }
        )
    }
}

@Composable
private fun TableHeader() {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Name",
                fontFamily = MonospaceFamily,
                fontSize = 12.sp,
                color = IncenseColors.Success,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(0.46f)
                    .fillMaxWidth()
            )
            Text(
                text = "Duration",
                fontFamily = MonospaceFamily,
                fontSize = 12.sp,
                color = IncenseColors.Success,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(0.22f)
                    .fillMaxWidth()
            )
            Text(
                text = "Today",
                fontFamily = MonospaceFamily,
                fontSize = 12.sp,
                color = IncenseColors.Success,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(0.16f)
                    .fillMaxWidth()
            )
            Text(
                text = "Edit",
                fontFamily = MonospaceFamily,
                fontSize = 12.sp,
                color = IncenseColors.Success,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(0.16f)
                    .fillMaxWidth()
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "====",
                fontFamily = MonospaceFamily,
                fontSize = 10.sp,
                color = IncenseColors.Success,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(0.46f)
                    .fillMaxWidth()
            )
            Text(
                text = "========",
                fontFamily = MonospaceFamily,
                fontSize = 10.sp,
                color = IncenseColors.Success,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(0.22f)
                    .fillMaxWidth()
            )
            Text(
                text = "=====",
                fontFamily = MonospaceFamily,
                fontSize = 10.sp,
                color = IncenseColors.Success,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(0.16f)
                    .fillMaxWidth()
            )
            Text(
                text = "====",
                fontFamily = MonospaceFamily,
                fontSize = 10.sp,
                color = IncenseColors.Success,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(0.16f)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WorkloadTableRow(
    workload: WorkloadRow,
    isSelected: Boolean,
    todayCount: Int,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (!isSelected) it.clickable(onClick = onSelect) else it }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val prefix = if (isSelected) ">" else " "
        Text(
            text = "$prefix ${workload.name.replace("_", " ")}",
            fontFamily = MonospaceFamily,
            fontSize = 13.sp,
            color = if (isSelected) IncenseColors.PrimaryText else IncenseColors.DimText,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .weight(0.46f)
                .fillMaxWidth(),
            maxLines = 1
        )
        Text(
            text = "${workload.defaultDurationMinutes}m",
            fontFamily = MonospaceFamily,
            fontSize = 13.sp,
            color = IncenseColors.DimText,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .weight(0.22f)
                .fillMaxWidth()
        )
        Text(
            text = "$todayCount",
            fontFamily = MonospaceFamily,
            fontSize = 13.sp,
            color = IncenseColors.DimText,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .weight(0.16f)
                .fillMaxWidth()
        )
        Text(
            text = "···",
            fontFamily = MonospaceFamily,
            fontSize = 13.sp,
            color = IncenseColors.Accent,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .weight(0.16f)
                .fillMaxWidth()
                .clickable(onClick = onEdit)
        )
    }
}

@Composable
private fun AddWorkloadDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, durationMinutes: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(0.dp),
            color = IncenseColors.Background,
            border = BorderStroke(1.dp, IncenseColors.Accent)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "=== new workload ===",
                    fontFamily = MonospaceFamily,
                    fontSize = 14.sp,
                    color = IncenseColors.Accent
                )
                Spacer(modifier = Modifier.height(16.dp))

                DialogTextField(
                    label = "Name    :",
                    value = name,
                    onValueChange = { name = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                DialogTextField(
                    label = "Duration:",
                    value = duration,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) duration = it },
                    placeholder = "minutes"
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "[ add ]",
                    fontFamily = MonospaceFamily,
                    fontSize = 13.sp,
                    color = IncenseColors.Success,
                    modifier = Modifier
                        .clickable {
                            val dur = duration.toIntOrNull()
                            if (name.isNotBlank() && dur != null && dur > 0) {
                                onAdd(name.trim(), dur)
                            }
                        }
                        .padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "[ cancel ]",
                    fontFamily = MonospaceFamily,
                    fontSize = 11.sp,
                    color = IncenseColors.DimText,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun EditWorkloadDialog(
    workload: WorkloadRow,
    onDismiss: () -> Unit,
    onSave: (name: String, durationMinutes: Int) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(workload.name) }
    var duration by remember { mutableStateOf(workload.defaultDurationMinutes.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(0.dp),
            color = IncenseColors.Background,
            border = BorderStroke(1.dp, IncenseColors.Accent)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "=== edit ===",
                    fontFamily = MonospaceFamily,
                    fontSize = 14.sp,
                    color = IncenseColors.Accent
                )
                Spacer(modifier = Modifier.height(16.dp))

                DialogTextField(
                    label = "Name    :",
                    value = name,
                    onValueChange = { name = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                DialogTextField(
                    label = "Duration:",
                    value = duration,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) duration = it },
                    placeholder = "minutes"
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "[ save ]",
                    fontFamily = MonospaceFamily,
                    fontSize = 13.sp,
                    color = IncenseColors.Success,
                    modifier = Modifier
                        .clickable {
                            val dur = duration.toIntOrNull()
                            if (name.isNotBlank() && dur != null && dur > 0) {
                                onSave(name.trim(), dur)
                            }
                        }
                        .padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "[ delete ]",
                    fontFamily = MonospaceFamily,
                    fontSize = 13.sp,
                    color = IncenseColors.Warning,
                    modifier = Modifier
                        .clickable(onClick = onDelete)
                        .padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "[ cancel ]",
                    fontFamily = MonospaceFamily,
                    fontSize = 11.sp,
                    color = IncenseColors.DimText,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DialogTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "name"
) {
    val textColor = IncenseColors.Ember
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontFamily = MonospaceFamily,
            fontSize = 13.sp,
            color = IncenseColors.DimText
        )
        Spacer(modifier = Modifier.width(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(120.dp),
            textStyle = TextStyle(
                color = textColor,
                fontFamily = MonospaceFamily,
                fontSize = 13.sp
            ),
            singleLine = true,
            cursorBrush = SolidColor(textColor)
        )
    }
}
