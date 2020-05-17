/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lavina;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import javax.swing.JOptionPane;
import java.util.*;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.Timer;

/**
 *
 * @author Bhupendra
 */
public class StartExamDialog extends javax.swing.JDialog implements ActionListener {

    //09-May-2017
    Timer tmr;
    String username, papercode;
    int seconds, totalseconds;
    //17-Apr-2017
    private static int totalquestions = 10;
    ResultSet rs;
    String sql;

    //18-Apr-2017
    ArrayList<Question> list, list1;
    int position = -1;
    //25-Apr-2017: Concept of Buttons
    JButton btn[];
    Color oldColor, defaultColor;
    JFrame mainframe;

    public StartExamDialog(JFrame f, boolean modal, String u, String p) {
        this(f, modal);
        mainframe = f;
        username = u;
        papercode = p;
        setTitle("Welcome " + u + " in Exam");
        btn = null;       //25-Apr-2017
        oldColor = btnPrevious.getBackground();
        defaultColor = btnPrevious.getBackground();
        initializeQuestions();  //User-Defined Fn     

        seconds = 0;
        totalseconds = totalquestions * 60;
        tmr = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                seconds++;
                lblTimer.setText("" + seconds);
                if (seconds == totalseconds) {
                    btnFinish.doClick();   //Programmatically click Finish
                    tmr.stop();
                }
            }
        });   //1sec, code class -OCJP
        tmr.start();
    }

    public StartExamDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

    }

    private void initializeQuestions() {
        //17-Apr-2017
        //1. Obtain total question to show in Exam
        totalquestions = DBConnection.getTotalQuestions();
        //2. Query DB for given papercode and check questions available or not
        sql = String.format("select count(qid) as total  from questions where category='%s' ", papercode);
        try {
            rs = DBConnection.executeQuery(sql);
            rs.next();
            int count = rs.getInt("total");
            if (count == 0 || count < totalquestions) {
                JOptionPane.showMessageDialog(null, "Sorry only " + count + " Questions Found");
                return;  //Terminate
            }
            //Otherwise, Pick all the questions of given papercode
            sql = String.format(
                    "Select * from questions where category='%s' ", papercode);

            //Store all questions into arraylist
            list1 = tableToArrayList(sql); //User-Defined Fn
            Collections.shuffle(list1);

            //19-Apr-2017 Adding only totalquestions form list1 into list
            list = new ArrayList<Question>();
            btn = new JButton[totalquestions];   //25-Apr-2017
            int width = 59, height = 23;
            Dimension d = new Dimension(width, height);
            for (int i = 0; i < totalquestions; i++) {
                list.add(list1.get(i));
                //Adding Button into Dialog Window -25-Apr-2017
                btn[i] = new JButton("Q" + (i + 1));
                btn[i].addActionListener(this);
                btn[i].setPreferredSize(d);
                btn[i].setVisible(true);
                // btn[i].setContentAreaFilled(false);
                btn[i].setOpaque(true);         //To Display Background Color
                panelButton.add(btn[i]);
            }

            list1 = null;  //OCJP1. Ref Count=0, Marked by AGC For GC
            //Display Question
            position = 0;
            displayQuestion(-1, position);
            // highlightButton(-1, position);   // -1 means no old position
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Initalize Question" + e.getMessage());
        }

    }

    private void highlightButton(int oldposition, int position) {
        btn[position].setEnabled(false);
        //btn[position].setBackground(Color.RED);
        btnPrevious.setEnabled(position > 0);
        btnNext.setEnabled(position < (totalquestions - 1));
        btnFinish.setEnabled(position == totalquestions - 1);
        //Reset Color of Previously Red button to Default
        if (oldposition == -1) {
            return;
        }
        btn[oldposition].setEnabled(true);
        //  btn[oldposition].setBackground(oldColor);

    }
//25-Apr-2017 Event code to handle Button Click Event

    public void actionPerformed(ActionEvent ae) {
        int oldposition = position;
        // oldColor = btn[oldposition].getBackground();
        position = Integer.parseInt(ae.getActionCommand().substring(1)) - 1;
        if (oldposition != -1) {
            preserveQuestion(oldposition);
        }
        displayQuestion(oldposition, position);
        // highlightButton(oldposition, position);
       

    }

    private void displayQuestion(int oldposition, int position) {
        Question q = list.get(position);   //Store current question in local variable
        lblQNo.setText("Q" + (position + 1));

        txtQuestion.setText(q.getQuestion());

        chkOp1.setText(q.getOp1());
        chkOp2.setText(q.getOp2());
        chkOp3.setText(q.getOp3());
        chkOp4.setText(q.getOp4());
        //Enable/Disable
        chkOp1.setSelected(q.getOpted().indexOf("1") > -1);
        chkOp2.setSelected(q.getOpted().indexOf("2") > -1);
        chkOp3.setSelected(q.getOpted().indexOf("3") > -1);
        chkOp4.setSelected(q.getOpted().indexOf("4") > -1);
        btn[position].setBackground(Color.RED);
        highlightButton(oldposition, position);  //Call Highlight Button
    }

    private ArrayList<Question> tableToArrayList(String sql) throws SQLException {
        ArrayList<Question> al = new ArrayList<>();
        ResultSet rs;  //Local Variable
        rs = DBConnection.executeQuery(sql);
        int qid, weight;
        String ques, op1, op2, op3, op4, ans, cat, opted;

        while (rs.next()) {
            qid = rs.getInt("qid");  //Store field into local variable
            ques = rs.getString("question");
            weight = rs.getInt("weight");
            op1 = rs.getString("op1");
            op2 = rs.getString("op2");
            op3 = rs.getString("op3");
            op4 = rs.getString("op4");
            ans = rs.getString("ans");
            cat = rs.getString("category");
            opted = "";  //Since No answer is selected By default
            al.add(new Question(qid, ques, op1, op2, op3, op4, cat, weight, ans, opted));
        }
        return al;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblRules = new javax.swing.JLabel();
        lblQNo = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtQuestion = new javax.swing.JTextArea();
        chkOp1 = new javax.swing.JCheckBox();
        chkOp2 = new javax.swing.JCheckBox();
        chkOp3 = new javax.swing.JCheckBox();
        chkOp4 = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        btnPrevious = new javax.swing.JButton();
        btnNext = new javax.swing.JButton();
        btnFinish = new javax.swing.JButton();
        panelButton = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        lblTimer = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(950, 500));

        lblRules.setForeground(new java.awt.Color(0, 51, 255));
        lblRules.setText("Show Rules and Guidelines");
        lblRules.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblRulesMouseClicked(evt);
            }
        });

        lblQNo.setText("Q");

        txtQuestion.setColumns(20);
        txtQuestion.setRows(5);
        jScrollPane1.setViewportView(txtQuestion);

        chkOp1.setText("Option 1");

        chkOp2.setText("Option 2");

        chkOp3.setText("Option 3");

        chkOp4.setText("Option 4");

        btnPrevious.setText("Previous");
        btnPrevious.setEnabled(false);
        btnPrevious.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navigationClick(evt);
            }
        });

        btnNext.setText("Next");
        btnNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navigationClick(evt);
            }
        });

        btnFinish.setText("Finish");
        btnFinish.setEnabled(false);
        btnFinish.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navigationClick(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnPrevious)
                .addGap(62, 62, 62)
                .addComponent(btnNext)
                .addGap(54, 54, 54)
                .addComponent(btnFinish)
                .addContainerGap(62, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnPrevious)
                    .addComponent(btnNext)
                    .addComponent(btnFinish))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelButton.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel1.setText("Time Elapsed in Seconds:");

        lblTimer.setText("0");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(131, 131, 131)
                        .addComponent(lblRules)
                        .addGap(36, 36, 36)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblTimer))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lblQNo)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 363, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addGroup(layout.createSequentialGroup()
                                            .addComponent(chkOp3)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(chkOp4))
                                        .addGroup(layout.createSequentialGroup()
                                            .addComponent(chkOp1)
                                            .addGap(138, 138, 138)
                                            .addComponent(chkOp2)))))
                            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(panelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 885, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(24, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblRules)
                    .addComponent(jLabel1)
                    .addComponent(lblTimer))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblQNo)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(chkOp1)
                    .addComponent(chkOp2))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(chkOp3)
                    .addComponent(chkOp4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panelButton, javax.swing.GroupLayout.DEFAULT_SIZE, 165, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void lblRulesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblRulesMouseClicked
        // TODO add your handling code here:
        new RulesDialog(null, true, username).setVisible(true);
    }//GEN-LAST:event_lblRulesMouseClicked

    private void navigationClick(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_navigationClick
        // TODO add your handling code here:
        preserveQuestion(position); //To save opted value of current question
        int oldposition = position;
        //oldColor = btn[oldposition].getBackground();
        switch (evt.getActionCommand()) {
            case "Previous":
                position = position - 1;
                displayQuestion(oldposition, position);
                break;
            case "Next":
                //if(position<)
                position = position + 1;
                displayQuestion(oldposition, position);
                break;
            case "Finish":
                int choice = JOptionPane.showConfirmDialog(null, "Are You Sure to Finish?", "Finish?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    calculateResult();
                    this.dispose();
                    // if (mainframe != null && mainframe.isDisplayable()) {
                    //    mainframe.setSize(800,600);
                    //    mainframe.setVisible(true);

                    //}
                    new LoginFrame().setVisible(true);
                }
                break;
        }
    }//GEN-LAST:event_navigationClick
    private void calculateResult() {
        //Local Variable
        int correct = 0, wrong = 0, notanswered = 0;
        double score = 0;

        //Step I: Check for correct, wrong, notanswered
        //Additional create query for exam_detal
        for (Question i : list) {
            if (i.getOpted().trim().length() == 0) {
                notanswered++;
            } else if (i.getOpted().equals(i.getAns())) {
                score = score + i.getWeight();
                correct++;
            } else {
                wrong++;
                score = score - i.getWeight() * 0.25;
            }
        }
        try {
            //Step I: Generate ExamId
            sql = "Select examid_seq.nextval as examid from dual";
            rs = DBConnection.executeQuery(sql);
            int examid = 0;
            if (rs.next()) {
                examid = rs.getInt("examid");
            }
            sql = String.format("Insert Into exam_master values('%s' , '%s' , sysdate, '%s', '%s' , '%s' , '%s' , '%s', '%s' ) ", username, papercode, totalquestions, correct, wrong, score, examid, notanswered);
            int result = DBConnection.executeUpdate(sql);
            if (result == 0) {
                JOptionPane.showMessageDialog(null, "Failed to Save Result. Contact with DB Admin");
                return;
            }
            //Otherwise
            DBConnection.commit(); //Make Master Entry Parmanent
            String opted = "";
            for (Question i : list) {
                opted = i.getOpted();
                if (opted.length() == 0) {
                    opted = "-";
                }
                sql = String.format("Insert Into exam_details values('%s' , '%s' , '%s')", examid, i.getQid(), opted);
                result = DBConnection.executeUpdate(sql);
                if (result == 0) {
                    JOptionPane.showMessageDialog(null, "Failed to Save Result. Contact with DB Admin");
                    return;
                }
            }
            DBConnection.commit();
            JOptionPane.showMessageDialog(null, "Congrates. Your Score Saved  ");
            generateCertificate();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Calculate Result  " + e.getMessage());
        }

    }
    //02-May-2017

    private boolean generateCertificate() throws IOException {
        File f = new File("temp");
        f.createNewFile();
        String path = f.getAbsolutePath();
        path = path.replace("temp", "");
        File f1 = new File(path + "header.txt");
        File f2 = new File(path + "footer.txt");
        if (!f1.exists() || !f2.exists()) {
            JOptionPane.showMessageDialog(null, "Generate Certificate. Unable to Find header or footer.txt ");
            return false;
        }
        //Otherwise - Prepare Certificate at Runtime
        StringBuilder sb = new StringBuilder(1024); //Mutability
        sb.append("<tr><td class=candidate>");
        sb.append(username);
        sb.append("</td></tr><tr><td>Upon Successful Completion of Online Exam of ");
        sb.append("</td></tr><tr><td class=candidate>");
        sb.append(papercode);
        sb.append("</td></tr><tr><td> Date:</td></tr><tr><td class=candidate>");
        SimpleDateFormat sf;
        sf = new SimpleDateFormat("EEE, dd-MMM-yyyy");   // HH:mm:ss ");

        sb.append(sf.format(new Date()));
        sb.append("</td></tr>");
        FileOutputStream fos = new FileOutputStream("mid.txt");
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        bos.write(sb.toString().getBytes());
        bos.close();
        fos.close();
        FileInputStream file1 = new FileInputStream(f1);  //header.txt
        FileInputStream file2 = new FileInputStream(path + "mid.txt");
        FileInputStream file3 = new FileInputStream(f2);  //footer
        //Merging Header and Mid File
        Vector<FileInputStream> stream = new Vector<>();
        stream.add(file1);
        stream.add(file2);
        stream.add(file3);
        Enumeration enumeration = stream.elements();

        SequenceInputStream sis = new SequenceInputStream(enumeration);
        BufferedInputStream bis = new BufferedInputStream(sis);

        FileOutputStream fos2 = new FileOutputStream(path + username + ".html");
        BufferedOutputStream bos2 = new BufferedOutputStream(fos2);
        byte b[] = new byte[1024];   //1KB

        int c;
        while ((c = bis.read(b)) > -1) {
            bos2.write(b, 0, c);   //start 0 byte upto length c
        }

        file1.close();
        file2.close();
        file3.close();
        sis.close();
        bis.close();
        bos2.close();
        fos2.close();
        JOptionPane.showMessageDialog(null, "Certificate Created at " + path + username + ".html");
        return true;
    }

    private void preserveQuestion(int position) {
        Question q = list.remove(position);  //Get Current questions from arraylist
        String opted = ""; //Local Variable  - Set

        if (chkOp1.isSelected()) {
            opted += "1";
        }
        if (chkOp2.isSelected()) {
            opted += "2";
        }
        if (chkOp3.isSelected()) {
            opted += "3";
        }
        if (chkOp4.isSelected()) {
            opted += "4";
        }
        //Set value of opted in object
        // JOptionPane.showMessageDialog(null, "Preserve " + opted);
        q.setOpted(opted);
        //Store question back to arraylist
        // list.ad
        list.add(position, q);
        if (opted.length() != 0) {
            btn[position].setBackground(Color.GREEN);
            btn[position].setOpaque(true);
            //JOptionPane.showMessageDialog(null, "Green");
        } else {
            btn[position].setBackground(defaultColor);
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());

                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(StartExamDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(StartExamDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(StartExamDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(StartExamDialog.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                StartExamDialog dialog = new StartExamDialog(new javax.swing.JFrame(), true, "jayesh", "Java");
                //   StartExamDialog dialog = new StartExamDialog(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnFinish;
    private javax.swing.JButton btnNext;
    private javax.swing.JButton btnPrevious;
    private javax.swing.JCheckBox chkOp1;
    private javax.swing.JCheckBox chkOp2;
    private javax.swing.JCheckBox chkOp3;
    private javax.swing.JCheckBox chkOp4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblQNo;
    private javax.swing.JLabel lblRules;
    private javax.swing.JLabel lblTimer;
    private javax.swing.JPanel panelButton;
    private javax.swing.JTextArea txtQuestion;
    // End of variables declaration//GEN-END:variables
}
