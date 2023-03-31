package com.code.bumbumapps.netspeed.CustomAdapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.code.bumbumapps.netspeed.MainActivity;
import com.code.bumbumapps.netspeed.R;
import com.code.bumbumapps.netspeed.ServersPreference;

import java.util.HashMap;
import java.util.List;

public class ServersAdapter extends RecyclerView.Adapter<ServersAdapter.ServerViewHolder>{

    Context context;
    HashMap<Integer, List<String>> mapValue;
    ServersPreference serversPreference;
    public ServersAdapter(Context context, HashMap<Integer,List<String>> mapValue){
        this.context=context;
        this.mapValue=mapValue;
    }

    @NonNull
    @Override
    public ServerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.servers,parent,false);
        return new ServerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServerViewHolder holder, int position) {
        holder.server_location.setText(mapValue.get(position).get(2));
        holder.server_name.setText(mapValue.get(position).get(5));

    }

    @Override
    public int getItemCount() {
        return mapValue.size();
    }



    public class ServerViewHolder extends RecyclerView.ViewHolder  {
        TextView server_name,server_location;
        public ServerViewHolder(@NonNull View itemView) {
            super(itemView);
            server_name=itemView.findViewById(R.id.servername);
            server_location=itemView.findViewById(R.id.serve_location);
            serversPreference=new ServersPreference(context);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    serversPreference.setInt(getAdapterPosition());
                    serversPreference.setBoolen(false);
                    Intent intent=new Intent(context, MainActivity.class);
                    context.startActivity(intent);
                }
            });

        }


    }
}
