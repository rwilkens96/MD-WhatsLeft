/************************************************************\
 *       Copyright (C) 2001 Appgen Personal Software        *
\************************************************************/

package com.moneydance.modules.features.whatsleft;

import com.moneydance.awt.*;
import com.moneydance.apps.md.controller.Common;
import com.moneydance.apps.md.model.*;
import com.moneydance.apps.md.controller.Util;

import java.io.*;
import java.util.*;
import javax.swing.*;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Window used for Account List interface
 * ------------------------------------------------------------------------
 */

public class AccountListWindow extends JFrame implements ActionListener {
	static private Main extension;
	private JTextArea accountListArea;
	private JButton clearButton;
	private JButton closeButton;
	private JTextField inputArea;
	private JComboBox AcctCombo;
	private ReminderSet Reminders;
	private static StringBuffer AcctsListing;
	public boolean debug=false;

	private Object makeObj(final String item) {
		return new Object() {
			public String toString() {
				return item;
			}
		};
	}

	public AccountListWindow(Main extension) {
		super("What's Left Console");
		this.extension = extension;
		AcctCombo = new JComboBox();
		accountListArea = new JTextArea();

		RootAccount root = extension.getUnprotectedContext().getRootAccount();
		Reminders=root.getReminderSet();
		StringBuffer acctStr = new StringBuffer();
		if (root != null) {
			addSubAccounts(root, acctStr);
		}
		accountListArea.setEditable(false);
		accountListArea.setText(acctStr.toString());
		if (AcctsListing != null) {
			while (AcctsListing.length() > 0) {
				String name = AcctsListing.toString();
				int where = name.indexOf("\n");
				AcctCombo.addItem(makeObj(name.substring(0, where)));
				AcctsListing = new StringBuffer(name.substring(where + 1));
				System.gc();
			}
		}
		if (AcctCombo.countComponents() > 0) {
			AcctCombo.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					AccountSelectedInCombo();
				}
			});
			AccountSelectedInCombo();
		} else {
			AcctCombo.disable();
		}
		inputArea = new JTextField();
		inputArea.setEditable(true);
		//clearButton = new JButton("Clear");
		closeButton = new JButton("Close");

		JLabel lbl = new JLabel("Select Bank Account:", SwingConstants.RIGHT);
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(new EmptyBorder(10, 10, 10, 10));
		p.add(lbl, AwtUtil.getConstraints(0, 0, 0, 0, 1, 1, true, false));
		p.add(AcctCombo, AwtUtil.getConstraints(1, 0, 0, 0, 1, 1, true, false));
		p.add(new JScrollPane(accountListArea),
				AwtUtil.getConstraints(0, 1, 1, 2, 2, 2, true, true));
		p.add(Box.createVerticalStrut(8),
				AwtUtil.getConstraints(0, 4, 0, 0, 1, 1, false, false));
		//p.add(clearButton,
		//		AwtUtil.getConstraints(0, 5, 1, 0, 1, 1, false, true));
		p.add(closeButton,
				AwtUtil.getConstraints(1, 5, 1, 0, 1, 1, false, true));
		getContentPane().add(p);

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		enableEvents(WindowEvent.WINDOW_CLOSING);
		closeButton.addActionListener(this);
		//clearButton.addActionListener(this);

		PrintStream c = new PrintStream(new ConsoleStream());

		setSize(500, 400);
		AwtUtil.centerWindow(this);
/*
        if (this.listeningToReminders == false) {
            reminderSet.addReminderListener(this.reminderListener);
            this.listeningToReminders = true;
        }
        
        if (this.listeningToTransactions == false) {
            transactionSet.addTransactionListener(this.transactionListener);
            this.listeningToTransactions = true;
        }
*/
	}

	private class ReminderListItem{
		public TransactionReminder Rem;
		public Calendar Cal;
		public int DaysAway;

	}
	public void AccountSelectedInCombo() {
		accountListArea.setEditable(true);
		accountListArea.setText("Account Selected: "
				+ AcctCombo.getSelectedItem().toString()+"\n\n");
		if (debug){
			accountListArea.append("Account Balance: ");
		}
		RootAccount root = extension.getUnprotectedContext().getRootAccount();
		Account SelAcct=root.getAccountByName(AcctCombo.getSelectedItem().toString());
		//accountListArea.append(String.valueOf(SelAcct.getCurrencyType().getPrefix()));
		DecimalFormat df = new DecimalFormat("#0.00");
		long StartingBal=SelAcct.getBalance();
		
		if (debug){
			accountListArea.append((df.format(SelAcct.getCurrencyType().getDoubleValue(StartingBal)))+"\n\n");
		}
        	Enumeration<Reminder> reminderEnumeration = Reminders.getAllReminders();
        	java.util.List<ReminderListItem> MyListReminders=new ArrayList<ReminderListItem>();
        	while (reminderEnumeration.hasMoreElements()) {
        	int daystocheck=60;
        	Calendar cal=Calendar.getInstance();
        	Date day=cal.getTime();
        	cal.setTime(day);//redundant oops	
		cal.set(Calendar.HOUR_OF_DAY,0);
            	Reminder reminder = reminderEnumeration.nextElement();
            	if (reminder instanceof TransactionReminder) {
                	TransactionReminder transactionReminder = (TransactionReminder) reminder;
                ParentTxn parentTxn =  transactionReminder.getTransaction();
                String accountName = parentTxn.getAccount().getAccountName();
                if (accountName.equals(AcctCombo.getSelectedItem().toString())){
                	 
                	for (int i=0;i<60;i++){
                		if (transactionReminder.occursOnDate(cal)){
                			
                			int LastDate=transactionReminder.getDateAcknowledgedInt();
                			int dayDt=cal.get(cal.YEAR)*10000;
                			dayDt+=(cal.get(cal.MONTH)+1)*100;
                			dayDt+=cal.get(cal.DATE);
                			if (dayDt<=LastDate){
						if (debug){
                    					accountListArea.append("ignoring:"+String.valueOf(LastDate)+" - Last:");
                    					accountListArea.append(String.valueOf(dayDt)+"\n\n");
						}
                			}else{
						if (debug){
                					accountListArea.append("adding: DaysAway:"+i);
						}
                				InsertIntoReminderList(MyListReminders,transactionReminder,cal.getTime(),i);
                			}
                			DateFormat dft=DateFormat.getDateInstance();
					if (debug){
                				accountListArea.append("Account Reminder: Date:");
                				accountListArea.append(dft.format(cal.getTime()));
            					accountListArea.append(":"+SelAcct.getCurrencyType().getDoubleValue(parentTxn.getValue())+"\n");
					}
                		}
                		cal.add(Calendar.DATE, 1);
                	}
                	
                }
            }
        }
        accountListArea.append("\nStarting Balance: "+df.format(SelAcct.getCurrencyType().getDoubleValue(StartingBal))+"\n");
        boolean IncomeFound=false;
        long runningbal=StartingBal;	
	if (debug){
        	accountListArea.append("\n\n(list size: "+MyListReminders.size()+")");
	}
        for (int i=0;i<MyListReminders.size();i++){
		if (debug){
			accountListArea.append("\ni="+i+"\n");
		}
        	if (!IncomeFound||debug){
				DateFormat dft=DateFormat.getDateInstance();
				TransactionReminder trm=MyListReminders.get(i).Rem;
				ParentTxn ptx=trm.getTransaction();
				
				if (debug){
					accountListArea.append("\nChecking:"+ptx.getValue());
				}
				if (ptx.getValue()<0){
					if (debug){
						accountListArea.append("\nExpense:");
						accountListArea.append(dft.format(MyListReminders.get(i).Cal.getTime()));
						accountListArea.append(", Amount:"+SelAcct.getCurrencyType().getPrefix()+
							df.format(SelAcct.getCurrencyType().getDoubleValue(ptx.getValue()))+" --");
					}
					if (!IncomeFound){
						runningbal+=ptx.getValue();
					} else{
						if (debug){
							accountListArea.append("(afterIncomeFound)");
						}
					}
					if (debug){
						accountListArea.append("Balance "+
							SelAcct.getCurrencyType().getPrefix()+
							df.format(SelAcct.getCurrencyType().getDoubleValue(runningbal))
							);
					}
				} else if (ptx.getValue()>0){
        				for (int j=i-1;j>=0;j--){
						DateFormat dft2=DateFormat.getDateInstance();
						TransactionReminder trm2=MyListReminders.get(j).Rem;
						ParentTxn ptx2=trm2.getTransaction();
						
						if 
						((MyListReminders.get(j).Cal.get(Calendar.DAY_OF_MONTH)==MyListReminders.get(i).Cal.get(Calendar.DAY_OF_MONTH))&&
						(MyListReminders.get(j).Cal.get(Calendar.MONTH)==MyListReminders.get(i).Cal.get(Calendar.MONTH))&&
						(MyListReminders.get(j).Cal.get(Calendar.YEAR)==MyListReminders.get(i).Cal.get(Calendar.YEAR))){
							
							runningbal-=ptx2.getValue();
							if (debug){
								accountListArea.append("\nDEDUCTED SAME DAY AS PAY DAY VALUE: "+ptx2.getValue());
								
							}

						}
					}

					//income
					
					if (debug){
						accountListArea.append("\nNext Income in "+MyListReminders.get(i).DaysAway+" days on ");
						accountListArea.append(dft.format(MyListReminders.get(i).Cal.getTime()));
						accountListArea.append(", Amount:"+SelAcct.getCurrencyType().getPrefix()+
							df.format(SelAcct.getCurrencyType().getDoubleValue(ptx.getValue()))+"\n\n");
					}
					accountListArea.append("Balance at next income:"+
							SelAcct.getCurrencyType().getPrefix()+
							df.format(SelAcct.getCurrencyType().getDoubleValue(runningbal))
							+"\n"
							);
					if (MyListReminders.get(i).DaysAway>0){
						IncomeFound=true;
						accountListArea.setLineWrap(true);
						accountListArea.append("\nAfter Scheduled Reminder Payments, \nYou have "+
							SelAcct.getCurrencyType().getPrefix()+
							df.format(SelAcct.getCurrencyType().getDoubleValue(runningbal))
							+" available in selected bank account \n(until next income in "
							+MyListReminders.get(i).DaysAway+" days.)\n\n"
							);
					
						accountListArea.append("Based on that Per day money available in this account: "+
							df.format(SelAcct.getCurrencyType().getDoubleValue(runningbal)/MyListReminders.get(i).DaysAway)+"\n\n");
					}else
					{
						if (debug){
							accountListArea.append("else!!\n\n");
						}
						runningbal+=ptx.getValue();
					}
					 
				}
        	}
        	
        }
    	if (!IncomeFound){
    		accountListArea.append("Beyond today, there is no found income scheduled \nin this account for next 60 days, so this extension can't help you here\nunless you schedule it in reminders.");
    	}
        accountListArea.setEditable(false);
		
		
		// works:AcctCombo.addItem(makeObj("Test"));
	}
	public void InsertIntoReminderList(java.util.List<ReminderListItem> TheList,Reminder TransRem, Date dat,int HowFar){
		boolean inserted=false;
		ReminderListItem El=new ReminderListItem();
		El.Cal=Calendar.getInstance();
		El.Cal.setTime(dat);
		El.Rem=(TransactionReminder)TransRem;
		El.DaysAway=HowFar;
		for (int i=0;i<TheList.size();i++){
			if (!inserted){
				if (TheList.get(i).Cal.after(El.Cal)){
					if (debug){
                				DateFormat dft=DateFormat.getDateInstance();
						accountListArea.append("(:)"+dft.format(TheList.get(i).Cal.getTime())+"(:)");
    						accountListArea.append("(Inserted 1 @ "+i+")");
					}
					TheList.add(i, El);
					inserted=true;
				}
				//the following is to make sure (Required) income is listed before expense
				if (!inserted){
					ParentTxn ptx=El.Rem.getTransaction();
					if (ptx.getValue()>0){
						Date Dt1,Dt2;
						Dt1=TheList.get(i).Cal.getTime();
						Dt2=El.Cal.getTime();
						
						if ((Dt1.getYear()==Dt2.getYear())&&
							(Dt1.getMonth()==Dt2.getMonth())&&
							(Dt1.getDay()==Dt2.getDay()))
						{
							
							TheList.add(i+1, El);
							inserted=true;
							if (debug){
    								accountListArea.append("(Inserted 2 @ "+(i+1)+")");
							}
						
						}
					}
				}
			}
		}
		if (!inserted){
			if (debug){
    				accountListArea.append("(Inserted 3)");
			}
			if (!TheList.add(El)){
    				accountListArea.append("(failure)");
			}
			inserted=true;//not necessary but in case we build on this later
		}
		
	}
	public static void addSubAccounts(Account parentAcct, StringBuffer acctStr) {
		acctStr.append("Bank Accounts :\n\n");
		int sz = parentAcct.getSubAccountCount();
		AcctsListing = new StringBuffer();

		for (int i = 0; i < sz; i++) {

			Account acct = parentAcct.getSubAccount(i);

			if (acct != null) {

				if (acct.getAccountType() == 1000) {
					if (acct.getHideOnHomePage() == false) {

						acctStr.append(acct.getFullAccountName() + "\n");
						AcctsListing.append(acct.getFullAccountName() + "\n");

					}
				}

			}
			// addSubAccounts(acct, acctStr);
		}
	}

	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();
		if (src == closeButton) {
			extension.closeConsole();
		}
		if (src == clearButton) {
			accountListArea.setText("");
		}
	}

	public final void processEvent(AWTEvent evt) {
		if (evt.getID() == WindowEvent.WINDOW_CLOSING) {
			extension.closeConsole();
			return;
		}
		if (evt.getID() == WindowEvent.WINDOW_OPENED) {
		}
		super.processEvent(evt);
	}

	private class ConsoleStream extends OutputStream implements Runnable {
		public void write(int b) throws IOException {
			accountListArea.append(String.valueOf((char) b));
			repaint();
		}

		public void write(byte[] b) throws IOException {
			accountListArea.append(new String(b));
			repaint();
		}

		public void run() {
			accountListArea.repaint();
		}
	}

	void goAway() {
		setVisible(false);
		dispose();
	}
}
