/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.lambdalib.vis.editor.common.widget;

import cn.lambdalib.cgui.gui.LIGui;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.annotations.GuiCallback;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.component.Transform.HeightAlign;
import cn.lambdalib.cgui.gui.component.Transform.WidthAlign;
import cn.lambdalib.cgui.gui.event.DragEvent;
import cn.lambdalib.cgui.gui.event.DragStopEvent;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.gui.event.GuiEvent;
import cn.lambdalib.cgui.gui.event.MouseDownEvent;
import cn.lambdalib.cgui.gui.event.RefreshEvent;
import cn.lambdalib.cgui.loader.EventLoader;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.vis.editor.common.VEVars;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
public class Window extends Widget {
	
	// Window-specific events decl
	public static class BodyStateChangeEvent implements GuiEvent {
		public final BodyState prev;
		public BodyStateChangeEvent(BodyState _prev) {
			prev = _prev;
		}
	}
	
	public enum BodyState { MINIMIZED, FULL };
	
	public enum TopButtonType { MINIMIZE, MAXIMIZE };
	
	public static final double
		WINDOW_TOP_HT = 12,
		DEF_WINDOW_WIDTH = 100,
		DEF_WINDOW_HT = 144,
		TOP_FT_SIZE = 10;
	
	public final Widget topArea, body;
	public final String name;
	private BodyState bodyState = BodyState.FULL;
	
	private int nTopBtnInit = 0;
	
	public Window(String _name) {
		name = _name;
		
		transform.width = DEF_WINDOW_WIDTH;
		transform.height = DEF_WINDOW_HT;
		transform.doesListenKey = false;
		
		/* Top Area */ {
			topArea = new Widget();
			topArea.transform.height = WINDOW_TOP_HT;
			topArea.listen(DragEvent.class, (w, event) ->
			{
				LIGui gui = w.getGui();
				double ax = gui.mouseX - event.offsetX, ay = gui.mouseY - event.offsetY;
				gui.moveWidgetToAbsPos(Window.this, ax, ay);
			});
			
			DrawTexture dt = new DrawTexture();
			dt.setTex(null);
			dt.color = VEVars.C_WINDOW_TOP;
			topArea.addComponent(dt);
			
			/* Text Drawing */ {
				Widget container = new Widget();
				container.transform.setSize(0, WINDOW_TOP_HT);
				container.transform.setPos(3, 2);
				container.transform.doesListenKey = false;
				
				TextBox tb = new TextBox();
				tb.heightAlign = HeightAlign.TOP;
				tb.content = name;
				tb.size = TOP_FT_SIZE;
				tb.color = VEVars.C_WINDOW_TEXT;
				
				container.addComponent(tb);
				topArea.addWidget(container);
			}
			
			addWidget("top", topArea);
		}
		
		/* Body */ {
			body = new Widget();
			body.transform.y = WINDOW_TOP_HT;
			
			DrawTexture dt = new DrawTexture();
			dt.setTex(null);
			dt.color = VEVars.C_WINDOW_BODY;
			body.addComponent(dt);
			
			addWidget("body", body);
		}
		
		EventLoader.load(this, this);
		//refreshArea();
	}
	
	Widget resizeArea;
	
	@Override
	public void onAdded() {
		resizeArea = new Widget() {
			boolean dragging = false;
			
			{
				Window window = Window.this;
				DrawTexture dt = new DrawTexture().setTex(TEX_BTN_RESIZE);
				
				double sz = 6, hsz = sz / 2;
				
				transform.setSize(sz, sz);
				listen(DragEvent.class, (__, event) -> 
				{
					double ax = getGui().mouseX - event.offsetX + hsz;
					double lx = (ax - Window.this.x) / Window.this.scale;
					lx = MathUtils.wrapd(20, window.transform.width * 1.5, lx);
					transform.x = window.transform.x + lx * window.transform.scale - hsz;
					transform.y = window.transform.y + lx * window.transform.scale * (window.transform.height / window.transform.width) - hsz;
					dirty = true;
					dragging = true;
				});
				
				listen(DragStopEvent.class, (__, event) -> 
				{
					double lx = (x - window.x) / window.scale;
					double nscale = window.transform.scale * lx / window.transform.width;
					window.transform.scale = MathUtils.wrapd(0.2, 1.5, nscale);
					window.dirty = true;
					dragging = false;
				});
				listen(FrameEvent.class, (__, event) ->
				{
					dt.color.a = event.hovering || dragging ? 1 : 0;
					
					if(!dragging) {
						transform.setPos(
							window.transform.x + window.transform.width * window.transform.scale - hsz, 
							window.transform.y + window.transform.height * window.transform.scale - hsz);
						dirty = true;
						if(window.disposed)
							dispose();
					}
				});
				addComponent(dt);
			}
		};
		this.getAbstractParent().addWidget(resizeArea);
	}
	
	static final ResourceLocation 
		TEX_BTN_MINIMIZE = VEVars.tex("buttons/minimize"),
		TEX_BTN_RESTORE = VEVars.tex("buttons/restore"),
		TEX_BTN_RESIZE = VEVars.tex("buttons/resize");
	
	public Widget initTopButton(TopButtonType type) {
		final double sz = 8, step = sz + 5;
		double x = -(5 + nTopBtnInit * step);
		Widget w;
		switch(type) {
		case MAXIMIZE:
			w = new Button(null, sz, sz);
			break;
		case MINIMIZE:
			w = new Button(bodyState == BodyState.FULL ? TEX_BTN_MINIMIZE : TEX_BTN_RESTORE, sz, sz);
			w.listen(MouseDownEvent.class, (widget, event) -> {
				DrawTexture dt = DrawTexture.get(widget);
				switch(bodyState) {
				case FULL:
					setBodyState(BodyState.MINIMIZED);
					dt.setTex(TEX_BTN_RESTORE);
					break;
				case MINIMIZED:
					setBodyState(BodyState.FULL);
					dt.setTex(TEX_BTN_MINIMIZE);
					break;
				}
			});
			break;
		default:
			throw new RuntimeException("What?!");
		}
		w.transform.setPos(x, 3).setSize(sz, sz);
		w.transform.alignWidth = WidthAlign.RIGHT;
		
		++nTopBtnInit;
		addWidget(w);
		return w;
	}
	
	public Widget initScrollBar() {
		// TODO Implement
		return null;
	}
	
	public BodyState getBodyState() {
		return bodyState;
	}
	
	public void setBodyState(BodyState newState) {
		if(newState != bodyState) {
			BodyState lastState = bodyState;
			bodyState = newState;
			post(new BodyStateChangeEvent(lastState));
		}
	}
	
	@GuiCallback
	public void onRefresh(Widget w, RefreshEvent event) {
		refreshArea();
	}
	
	@GuiCallback
	public void updateBodySz(Widget w, BodyStateChangeEvent event) {
		if(bodyState == BodyState.FULL) {
			body.transform.doesDraw = true;
		} else {
			body.transform.doesDraw = false;
		}
		this.dirty = true;
	}
	
	private void refreshArea() {
		topArea.transform.width = this.transform.width;
		body.transform.width = this.transform.width;
		body.transform.height = this.transform.height - WINDOW_TOP_HT;
	}
	
}
