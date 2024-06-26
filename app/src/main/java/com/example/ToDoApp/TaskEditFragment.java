package com.example.ToDoApp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.ToDoApp.entities.Task;
import com.example.ToDoApp.viewmodel.TaskViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;

public class TaskEditFragment extends Fragment {

    private TaskViewModel mTaskViewModel;
    private DatePickerDialog datePickerDialog;
    private EditText nameEditText;
    private EditText descriptionEditText;
    private CheckBox doneCheckBox;
    private Button dateButton;
    private String date;
    private boolean isNewTask = true;
    private int taskId = -1;  // Task ID
    private int taskListId = -1;  // Task List ID

    private Observer<Task> taskObserver;

    public TaskEditFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTaskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        if (getArguments() != null) {
            taskId = getArguments().getInt("TASK_ID", -1);
            taskListId = getArguments().getInt("TASK_LIST_ID", -1);  // Retrieve the TaskList ID
        }

        // Observe selected task list ID
        mTaskViewModel.getSelectedTaskListId().observe(this, selectedId -> {
            if (selectedId != null) {
                taskListId = selectedId;
                mTaskViewModel.setFilter(mTaskViewModel.getAllTasks(taskListId));
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_edit, container, false);

        nameEditText = view.findViewById(R.id.textInputEditText2);
        descriptionEditText = view.findViewById(R.id.textInputEditText);
        doneCheckBox = view.findViewById(R.id.checkBox);
        dateButton = view.findViewById(R.id.button);

        initDatePicker();
        dateButton.setText(getCurrentDate());
        dateButton.setOnClickListener(v -> openDatePicker());

        if (taskId != -1) {
            isNewTask = false;
            taskObserver = task -> {
                if (task != null) {
                    nameEditText.setText(task.getName());
                    doneCheckBox.setChecked(task.isDone());
                    descriptionEditText.setText(task.getDescription());
                    dateButton.setText(task.getDate());
                    taskListId = task.getTaskListId();
                }
            };
            mTaskViewModel.getTaskById(taskId).observe(getViewLifecycleOwner(), taskObserver);
        }

        FloatingActionButton saveButton = view.findViewById(R.id.floatingActionButton);
        saveButton.setOnClickListener(v -> onClickSaveButton());

        return view;
    }

    private void initDatePicker() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year1, month1, dayOfMonth) -> {
                    month1 = month1 + 1;
                    date = makeDateString(dayOfMonth, month1, year1);
                    dateButton.setText(date);
                }, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(cal.getTimeInMillis());
        datePickerDialog.setTitle("Select Date");
    }

    private void openDatePicker() {
        datePickerDialog.show();
    }

    private String makeDateString(int day, int month, int year) {
        return month + "-" + day + "-" + year;
    }

    private String getCurrentDate() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return month + "-" + day + "-" + year;
    }

    private void onClickSaveButton() {
        String name = nameEditText.getText().toString();
        String description = descriptionEditText.getText().toString();
        boolean isDone = doneCheckBox.isChecked();
        String date = dateButton.getText().toString();

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Task name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (taskListId == -1) {
            // Handle the case when taskListId is not set
            Toast.makeText(getContext(), "Task List ID is invalid", Toast.LENGTH_SHORT).show();
            return;
        }

        Task task = new Task(name, description, date, isDone, taskListId);

        if (isNewTask) {
            mTaskViewModel.insert(task);
        } else {
            task.setId(taskId);
            mTaskViewModel.update(task);
        }

        mTaskViewModel.setSelectedTaskListId(taskListId);

        if (!getResources().getBoolean(R.bool.isTablet)) {
            requireActivity().getSupportFragmentManager().popBackStack();
        } else {
            // Remove observers to prevent old data being loaded again
            if (taskObserver != null) {
                mTaskViewModel.getTaskById(taskId).removeObserver(taskObserver);
            }
            // Reset the fragment for a clean state
            resetTaskDetails();

            TaskEditFragment fragment = new TaskEditFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("TASK_LIST_ID", taskListId);  // Pass the selected TaskList ID
            fragment.setArguments(bundle);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.edit_fragment, fragment)
                    .addToBackStack(null)
                    .commit();

        }
    }

    private void resetTaskDetails() {
        nameEditText.setText("");
        descriptionEditText.setText("");
        doneCheckBox.setChecked(false);
        dateButton.setText(getCurrentDate());

        taskId = -1;
        isNewTask = true;
    }
}
