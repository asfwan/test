package com.umranium.ebook;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioButton;

import com.actionbarsherlock.app.SherlockFragment;
import com.umranium.ebook.R;
import com.umranium.ebook.R.id;
import com.umranium.ebook.R.layout;
import com.umranium.ebook.model.UserDetails;
import com.umranium.ebook.services.IWebHostService;

public class LoginFragment extends SherlockFragment {

    WebHostServiceClientFragmentActivity activity;
    IWebHostService webHostService;
    AccountManager accountManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        activity = (WebHostServiceClientFragmentActivity) getActivity();
        webHostService = activity.getWebHostService();

        accountManager = (AccountManager) activity.getSystemService(Context.ACCOUNT_SERVICE);

        Account[] accounts = accountManager.getAccounts();
        UserDetails[] usersDetails = new UserDetails[accounts.length];

        for (int i = 0; i < accounts.length; ++i) {
            Account account = accounts[i];
            usersDetails[i] = webHostService.getUser(account.name);
        }

        View view = inflater.inflate(R.layout.main_login, container);

        ListView accountList = (ListView) view.findViewById(R.id.main_login_accountlist);
        accountList.setAdapter(new AccountListAdapter(inflater, accounts, usersDetails));

        return view;
    }

    private class AccountListAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private Account[] accounts;
        private UserDetails[] usersDetails;

        public AccountListAdapter(LayoutInflater inflater, Account[] accounts, UserDetails[] usersDetails) {
            this.accounts = accounts;
            this.usersDetails = usersDetails;
        }

        @Override
        public int getCount() {
            return accounts.length;
        }

        @Override
        public Object getItem(int position) {
            return accounts[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (usersDetails[position] != null) {
                return 0;
            } else {
                return 1;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (usersDetails[position] != null) {
                convertView = inflater.inflate(R.layout.main_login_row_known, parent, false);
            } else {
                convertView = inflater.inflate(R.layout.main_login_row_unknown, parent, false);
            }

            RadioButton username = (RadioButton) convertView.findViewById(R.id.main_login_row_username);
            username.setText(accounts[position].name);

//			if (usersDetails[position]!=null) {
//				TextView details = (TextView)convertView.findViewById(R.id.main_login_row_details);
//				details.setText(usersDetails[position]);
//			}

            return null;
        }

    }

}
