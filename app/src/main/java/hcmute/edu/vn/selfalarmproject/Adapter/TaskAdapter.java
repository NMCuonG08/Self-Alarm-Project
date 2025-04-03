package hcmute.edu.vn.selfalarmproject.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<String> taskList;
    private Context context;
    private OnTaskInteractionListener listener;

    // Interface to handle task interactions (edit/delete)
    public interface OnTaskInteractionListener {
        void onEditTask(int position);
        void onDeleteTask(int position);
    }

    public TaskAdapter(List<String> taskList, Context context, OnTaskInteractionListener listener) {
        this.taskList = taskList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        String task = taskList.get(position);
        holder.taskText.setText(task);

        // Handling edit and delete task events
        holder.itemView.setOnClickListener(v -> {
            // Open edit or delete task (example: clicking on task to edit)
            listener.onEditTask(position);
        });

        holder.itemView.setOnLongClickListener(v -> {
            // Long click for delete
            listener.onDeleteTask(position);
            Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskText;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskText = itemView.findViewById(android.R.id.text1);
        }
    }
}
