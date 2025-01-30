package userinterfaces;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import dao.BancoDados;
import dao.InscricaoDAO;
import entities.Evento;
import entities.Inscricao;
import entities.Participante;

public class TelaRelatorioParticipantesEventosPassadosAdmin extends JFrame {
    private static final long serialVersionUID = 1L;
    private JTable table;
    private DefaultTableModel model;

    public TelaRelatorioParticipantesEventosPassadosAdmin(boolean isAdmin) {
        if (!isAdmin) {
            JOptionPane.showMessageDialog(null, "Acesso negado! Somente administradores podem visualizar esta tela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            dispose();
            return;
        }

        setTitle("Relatório de Participantes dos Eventos Encerrados");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        JLabel titulo = new JLabel("Relatório de Participantes dos Eventos Encerrados", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        add(titulo, BorderLayout.NORTH);
        model = new DefaultTableModel() {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        model.addColumn("Nome");
        model.addColumn("Email");
        model.addColumn("CPF");
        model.addColumn("Data de Nascimento");
        model.addColumn("Evento");
        int[] columnWidths = { 200, 250, 150, 150, 200 };
        for (int i = 0; i < columnWidths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
        }
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, renderer);
        carregarDados();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new EmptyBorder(10, 10, 0, 10));
        add(scrollPane, BorderLayout.CENTER);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnVoltar = criarBotao("Voltar", e -> dispose());
        JButton btnExportar = criarBotao("Exportar .xls", e -> exportarParaXLS());
        btnExportar.setBackground(Color.getHSBColor(0.33f, 1f, 0.5f));
        btnExportar.setForeground(Color.WHITE);
        panel.add(btnVoltar);
        panel.add(btnExportar);
        add(panel, BorderLayout.SOUTH);
    }

    private void carregarDados() {
        try (Connection conn = BancoDados.conectar()) {
            InscricaoDAO inscricaoDAO = new InscricaoDAO(conn);
            List<Inscricao> inscricoes = inscricaoDAO.listarInscricoesEncerradas();
            for (Inscricao inscricao : inscricoes) {
                Evento evento = inscricao.getEvento();
                Participante participante = inscricao.getParticipante();
                model.addRow(new Object[] {
                    participante.getNomeCompleto(), participante.getEmail(), participante.getCpf(),
                    participante.getDataNascimento(), evento.getTitulo()
                });
            }
        } catch (SQLException | IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao buscar participantes: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            try {
                BancoDados.desconectar();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private JButton criarBotao(String texto, ActionListener acao) {
        JButton botao = new JButton(texto);
        botao.addActionListener(acao);
        return botao;
    }

    private void exportarParaXLS() {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "A tabela está vazia. Não há dados para exportar.", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar como");
        fileChooser.setSelectedFile(new File("relatorio_participantes.xls"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileWriter fw = new FileWriter(fileChooser.getSelectedFile()); BufferedWriter bw = new BufferedWriter(fw)) {
                for (int i = 0; i < model.getColumnCount(); i++) {
                    bw.write(model.getColumnName(i) + "\t");
                }
                bw.write("\n");

                for (int i = 0; i < model.getRowCount(); i++) {
                    for (int j = 0; j < model.getColumnCount(); j++) {
                        Object value = model.getValueAt(i, j);
                        bw.write((value != null ? value.toString() : "") + "\t");
                    }
                    bw.write("\n");
                }

                bw.flush();
                JOptionPane.showMessageDialog(this, "Relatório foi salvo com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Erro ao exportar relatório: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
}