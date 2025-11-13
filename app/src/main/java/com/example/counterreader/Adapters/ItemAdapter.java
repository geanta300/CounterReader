package com.example.counterreader.Adapters;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.counterreader.Helpers.DatabaseHelper;
import com.example.counterreader.Viewholders.ItemViewHolder;
import com.example.counterreader.R;
import com.squareup.picasso.Picasso;

import java.util.Objects;
import android.text.TextUtils;
import android.net.Uri;

public class ItemAdapter extends RecyclerView.Adapter<ItemViewHolder>{
    private final Cursor cursor;

    public ItemAdapter(Cursor cursor) {
        this.cursor = cursor;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        return new ItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        if (!cursor.moveToPosition(position)) {
            return;
        }

        // Extract the data from the cursor for the current position
        String chirias = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CHIRIAS));
        String locatie = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LOCATIE));
        String felContor = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FEL_CONTOR));
        String serie = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SERIE));
        double indexVechi = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INDEX_VECHI));
        double indexNou = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INDEX_NOU));
        String photoUri = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IMAGE_URI));

        // Format the data and set it in the TextViews using string resources with placeholders
        String chiriasText = holder.itemView.getContext().getString(R.string.chirias_text, chirias);
        String locatieText = holder.itemView.getContext().getString(R.string.locatie_text, locatie);
        String felContorText = holder.itemView.getContext().getString(R.string.fel_contor_text, felContor);
        String serieText = holder.itemView.getContext().getString(R.string.serie_text, serie);
        String indexVechiText = holder.itemView.getContext().getString(R.string.index_vechi_text, indexVechi);
        String indexNouText = holder.itemView.getContext().getString(R.string.index_nou_text, indexNou);

        holder.chiriasTextView.setText(chiriasText);
        holder.locatieTextView.setText(locatieText);
        holder.felContorTextView.setText(felContorText);
        holder.serieTextView.setText(serieText);
        holder.indexVechiTextView.setText(indexVechiText);
        holder.indexNouTextView.setText(indexNouText);
        if (!TextUtils.isEmpty(photoUri) && photoUri.trim().length() > 0) {
            holder.photoImageView.setVisibility(View.VISIBLE);
            Picasso.get().load(Uri.parse(photoUri)).into(holder.photoImageView);
        } else {
            holder.photoImageView.setVisibility(View.GONE);
        }

    }

    @Override
    public int getItemCount() {
        if (cursor != null) {
            return cursor.getCount();
        }
        return 0;
    }
}
