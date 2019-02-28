package app.com.detectionapp.PrommeInfo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import app.com.detectionapp.R;
import app.com.detectionapp.showProinfoActivity;

import static android.content.ContentValues.TAG;

/**
 * author : test
 * date : 2019/1/15 16:21
 * description :
 */


public class ProgrammeAdapter extends RecyclerView.Adapter<ProgrammeAdapter.ViewHolder>
{
    private List<Programme> mProgramList;
    private Context mcontext;
    static class ViewHolder extends RecyclerView.ViewHolder
    {
        ImageView program_icon;
        TextView  program_name;
        View      programView;


        public ViewHolder(View itemView) {
            super(itemView);
            programView = itemView;
            program_icon =  (ImageView) itemView.findViewById(R.id.program_icon);
            program_name = (TextView) itemView.findViewById(R.id.program_name);
        }
    }

    public ProgrammeAdapter(Context context, List<Programme> programmes)
    {
        mcontext = context;
        mProgramList = programmes;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mcontext).inflate(R.layout.program_item, parent, false);
        final ViewHolder holder = new ViewHolder(view);

        holder.programView.setOnClickListener(v->
        {
                int position = holder.getAdapterPosition();
                Programme programme = mProgramList.get(position);

                Toast.makeText(v.getContext(), programme.get_name(), Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(view.getContext(), showProinfoActivity.class);
                intent.putExtra("process_name", programme.get_process_name());
                intent.putExtra("application_name", programme.get_name());

                Log.d(TAG, "onCreateViewHolder: jiangzhe " + programme.get_pid());
                intent.putExtra("pid", String.valueOf(programme.get_pid()));
                ((Activity)v.getContext()).startActivityForResult(intent, Activity.RESULT_FIRST_USER);
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Programme programme = mProgramList.get(position);
        holder.program_name.setText(programme.get_name());
        if (programme.get_icon() != null)
            holder.program_icon.setImageDrawable(programme.get_icon());
        else{
            holder.program_icon.setImageResource(R.mipmap.ic_launcher);
        }
    }

    @Override
    public int getItemCount() {
        return  mProgramList.size();
    }

}
