package com.tonydicola.bletest.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class PatientListAdapter extends ArrayAdapter<Patient> {
    private LayoutInflater mInflater;
    public PatientListAdapter(Context context, int resource, List<Patient> objects) {
        super(context, resource, objects);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    public View getView (int position, View convertView, ViewGroup parent) {
        Patient patient = (Patient)getItem(position);
        View view = mInflater.inflate(R.layout.list_item, null);
        TextView firstName;
        firstName = (TextView) view.findViewById(R.id.firstName);
        firstName.setText(patient.firstName);

        TextView lastName;
        lastName = (TextView) view.findViewById(R.id.lastName);
        lastName.setText(patient.lastName);

        TextView DOB;
        DOB = (TextView) view.findViewById(R.id.DOB);
        DOB.setText(patient.DOB);

        return view;
    }
}
