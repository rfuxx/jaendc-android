package de.westfalen.fuldix.jaendc.manage;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import de.westfalen.fuldix.jaendc.R;
import de.westfalen.fuldix.jaendc.db.NDFilterDAO;
import de.westfalen.fuldix.jaendc.model.NDFilter;

import java.text.ParseException;

public class NDFilterEditController {
    private final NDFilter filter;
    private final EditText nameBox;
    private final EditText ndBox;
    private final EditText factorBox;
    private final EditText fstopsBox;
    private final NDFilterDAO dao;
    private final NDFilterDetailFragment.Callbacks callbacks;
    private boolean isUpdating = false;

    public NDFilterEditController(View rootView, NDFilter inFilter, NDFilterDAO dao, NDFilterDetailFragment.Callbacks callbacks) {
        this.filter = inFilter;
        this.dao = dao;
        this.callbacks = callbacks;
        nameBox = (EditText) rootView.findViewById(R.id.edit_name);
        ndBox = (EditText) rootView.findViewById(R.id.edit_nd);
        factorBox = (EditText) rootView.findViewById(R.id.edit_factor);
        fstopsBox = (EditText) rootView.findViewById(R.id.edit_fstops);
        nameBox.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                filter.setName(s.toString());
            }
        });
        ndBox.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                if (!isUpdating) {
                    isUpdating = true;
                    try {
                        double nd = filter.textEnterFormat.parse(s.toString().replace(',', '.')).doubleValue();
                        if(NDFilter.isValidND(nd)){
                            filter.setND(nd);
                            factorBox.setText(filter.decimalFormat.format(filter.getFactor()));
                            fstopsBox.setText(filter.decimalFormat.format(filter.getFstops()));
                        } else {
                            factorBox.setText("");
                            fstopsBox.setText("");
                        }
                    } catch (ParseException e) {
                        factorBox.setText("");
                        fstopsBox.setText("");
                    }
                    saveIfPossible();
                    isUpdating = false;
                }
            }
        });
        factorBox.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                if (!isUpdating) {
                    isUpdating = true;
                    try {
                        Number factorNumber = filter.textEnterFormat.parse(s.toString().replace(',', '.'));
                        int factor = factorNumber.intValue();
                        if(factorNumber.doubleValue() <= Integer.MAX_VALUE && NDFilter.isValidFactor(factor)) {
                            filter.setFactor(factor);
                            ndBox.setText(filter.decimalFormat.format(filter.getND()));
                            fstopsBox.setText(filter.decimalFormat.format(filter.getFstops()));
                        } else {
                            ndBox.setText("");
                            fstopsBox.setText("");
                        }
                    } catch (ParseException e) {
                        ndBox.setText("");
                        fstopsBox.setText("");
                    }
                    saveIfPossible();
                    isUpdating = false;
                }
            }
        });
        fstopsBox.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                if (!isUpdating) {
                    isUpdating = true;
                    try {
                        double fstops = filter.textEnterFormat.parse(s.toString().replace(',', '.')).doubleValue();
                        if(NDFilter.isValidFstops(fstops)) {
                            filter.setFStops(fstops);
                            ndBox.setText(filter.decimalFormat.format(filter.getND()));
                            factorBox.setText(filter.decimalFormat.format(filter.getFactor()));
                        } else {
                            ndBox.setText("");
                            factorBox.setText("");
                        }
                    } catch (ParseException e) {
                        ndBox.setText("");
                        factorBox.setText("");
                    }
                    saveIfPossible();
                    isUpdating = false;
                }
            }
        });
        View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                val2ui();
            }
        };
        nameBox.setOnFocusChangeListener(focusListener);
        ndBox.setOnFocusChangeListener(focusListener);
        factorBox.setOnFocusChangeListener(focusListener);
        fstopsBox.setOnFocusChangeListener(focusListener);
        val2ui();
    }

    void val2ui() {
        isUpdating = true;
        nameBox.setText(filter.getName());
        if(filter.isValidFactor(filter.getFactor())) {
            ndBox.setText(filter.decimalFormat.format(filter.getND()));
            factorBox.setText(filter.decimalFormat.format(filter.getFactor()));
            fstopsBox.setText(filter.decimalFormat.format(filter.getFstops()));
        }
        isUpdating = false;
    }

    public void saveIfPossible() {
        if(filter.isValidFactor(filter.getFactor())) {
            dao.storeNDFilter(filter);
            callbacks.onEditSaved(filter);
        }
    }
}
