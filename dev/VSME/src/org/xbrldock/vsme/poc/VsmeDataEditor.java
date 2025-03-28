package org.xbrldock.vsme.poc;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.xbrldock.XbrlDockException;
import com.xbrldock.utils.XbrlDockUtils;
import com.xbrldock.utils.XbrlDockUtilsGui;

@SuppressWarnings({ "rawtypes" })
public class VsmeDataEditor implements VsmePocConsts, XbrlDockUtilsGui.WidgetContainer {
	EditContext ctx;

	JComponent comp;

	@Override
	public JComponent getWidget() {
		return comp;
	}
	
	class DataItemListener implements ItemListener {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (ItemEvent.SELECTED == e.getStateChange()) {
				ctx.setReportValue(e.getItem(), (String) ((JComboBox) e.getSource()).getClientProperty(XDC_EXT_TOKEN_id), -1, null);
			}
		}
	};


	class DataDocumentListener implements DocumentListener {
		JTextField fld;
		String value;

		Runnable resetValue = new Runnable() {
			@Override
			public void run() {
				fld.setText(value);
			}
		};

		@Override
		public void removeUpdate(DocumentEvent e) {
			updateValue(e);
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			updateValue(e);
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			updateValue(e);
		}

		void updateValue(DocumentEvent e) {
			try {
				javax.swing.text.Document doc = e.getDocument();
				String id = (String) doc.getProperty(XDC_EXT_TOKEN_id);

				if (null != id) {
					String s = doc.getText(0, doc.getLength());

					if (XbrlDockUtils.isEmpty(s)) {
						ctx.setReportValue(null, id);
					} else {
						Object val = s;

						try {
							switch ((String) doc.getProperty(XDC_EXT_TOKEN_type)) {
							case "Real":
								val = Double.parseDouble(s);
								break;
							case "Int":
								val = Long.parseLong(s);
								break;
							}
						} catch (Throwable t) {
							value = XbrlDockUtils.toString(ctx.getReport().get(id));
							fld = (JTextField) doc.getProperty(XDC_CFGTOKEN_gui);
							java.awt.Toolkit.getDefaultToolkit().beep();
							SwingUtilities.invokeLater(resetValue);
						}

						ctx.setReportValue(val, id);
					}
				}
			} catch (Throwable e1) {
				XbrlDockException.wrap(e1);
			}
		}
	};


	public VsmeDataEditor(EditContext ctx, String i, Map<String, Object> attDef) {
		this.ctx = ctx;
		String attType = XbrlDockUtils.simpleGet(attDef, XDC_EXT_TOKEN_type);

		switch (attType) {
		case "String":
		case "Real":
		case "AttRef":
			comp = createTextField(i, attType, attDef);
			break;
		case "Identifier":
			comp = createSelector(i, attDef);
			break;
		default:
			XbrlDockException.wrap(null, "Should not be here", i, attDef);
			break;
		}

		Object val = ctx.getReport().get(i);
		if (null != val) {
			setGuiValue(val);
		}
	}

	public void setGuiValue(Object value) {
		Object w = comp;

		if (w instanceof JTextField) {
			((JTextField) w).setText(XbrlDockUtils.toString(value));
		} else if (w instanceof JComboBox) {
			((JComboBox) w).setSelectedItem(value);
		} else if (w instanceof ButtonGroup) {
			for (Enumeration<AbstractButton> eb = ((ButtonGroup) w).getElements(); eb.hasMoreElements();) {
				AbstractButton ab = eb.nextElement();
				ab.setSelected(XbrlDockUtils.isEqual(value, ab.getActionCommand()));
			}
		}
	};

	public JComponent createSelector(String i, Map<String, Object> attDef) {
		JComponent ret = null;

		Collection<Object> options = XbrlDockUtils.simpleGet(attDef, VSME_options);
		if (null != options) {
			JComboBox<String> cb = new JComboBox<String>();

			if (XbrlDockUtils.checkFlag(attDef, "AllowsText")) {
				cb.setEditable(true);
			}

			for (Object o : options) {
				String id = XbrlDockUtils.simpleGet(o, XDC_EXT_TOKEN_id);
				cb.addItem(id);
			}
			cb.setSelectedIndex(-1);
			cb.addItemListener(new DataItemListener());

			ret = cb;
		} else {
			JTextField tf = new JTextField();
			tf.setEditable(false);
		}

		return ret;

	}

	public JComponent createTextField(String i, String attType, Map<String, Object> attDef) {
		JTextField tf = new JTextField();

		switch (attType) {
		case "Real":
		case "Int":
			tf.setHorizontalAlignment(JTextField.RIGHT);
			break;
		}

		if (XbrlDockUtils.checkFlag(attDef, "Calculated")) {
			tf.setEditable(false);
		} else {
			javax.swing.text.Document doc = tf.getDocument();
			doc.addDocumentListener(new DataDocumentListener());
			doc.putProperty(XDC_EXT_TOKEN_id, i);
			doc.putProperty(XDC_EXT_TOKEN_type, attType);
			doc.putProperty(XDC_CFGTOKEN_gui, tf);
		}

		return tf;
	}

}
