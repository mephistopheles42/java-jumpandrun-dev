package de.secondsystem.game01.impl.editor.curser;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsfml.graphics.RenderTarget;
import org.jsfml.system.Vector2f;

import de.secondsystem.game01.impl.editor.LayerPanel.IOnLayerChangedListener;
import de.secondsystem.game01.impl.gui.ThumbnailButton;
import de.secondsystem.game01.impl.map.ILayerObject;
import de.secondsystem.game01.impl.map.IMapProvider;
import de.secondsystem.game01.impl.map.LayerType;
import de.secondsystem.game01.impl.map.IGameMap.WorldId;
import de.secondsystem.game01.model.IDrawable;

public final class CurserManager implements IOnLayerChangedListener, IDrawable {
	
	public static interface ISelectionChangedListener {
		void onSelectionChanged(IEditorCurser newSelection);
	}
	
	private final Set<ISelectionChangedListener> listeners = new HashSet<>();
	
	private final IMapProvider mapProvider;
	
	private final BrushPalette brushes = new BrushPalette();
	
	private LayerType layer;
	
	private IEditorCurser curser;
	
	public CurserManager(IMapProvider mapProvider) {
		this.mapProvider = mapProvider;
	}
	
	public void addListerner(ISelectionChangedListener listener) {
		listeners.add(listener);
		listener.onSelectionChanged(get());
	}
	protected void callListeners() {
		for( ISelectionChangedListener l : listeners )
			l.onSelectionChanged(curser);
	}
	protected void setCurser( IEditorCurser newCurser ) {
		if( curser!=newCurser ) {
			if( curser!=null )
				curser.onDestroy();
			
			curser = newCurser;
		}
		
		callListeners();
	}
	
	public IEditorCurser get() {
		return curser;
	}
	
	@Override
	public void onLayerChanged(LayerType layer) {
		this.layer = layer;
		setToBrush();
	}
	
	public void deleteSelected() {
		if( curser instanceof SelectionCurser ) {
			mapProvider.getMap().remove(layer, ((SelectionCurser)curser).getLayerObject());
			curser=null;
			setToBrush();
		}
	}

	public void setSelectionFromCurser(Vector2f mouse, LayerType layer) {
		List<ILayerObject> objs = mapProvider.getMap().findNodes(layer, mouse);
		
		objs.remove(((AbstractCurser)curser).getLayerObject());
		
		if( !objs.isEmpty() ) {
			setCurser(new SelectionCurser(mapProvider, objs));
		
		} else if( curser instanceof AbstractCurser )
			setToBrush();
	}
	
	public void setToNull() {
		if( curser!=null )
			curser.onDestroy();
		
		curser=null;
	}
	
	public void setToBrush() {
		setCurser(new BrushCurser(mapProvider, brushes.getBrush(mapProvider.getMap(), layer)));
	}


	public void scrollBrushes(boolean up) {
		((AbstractCurser) curser).cirlce(up);
		callListeners();
	}
	
	public int getCurrentBrushIndex() {
		return curser!=null ? curser.getCurrentBrushIndex() : 0;
	}
	public int getBrushCount() {
		return curser!=null ? curser.getBrushCount() : 0;
	}
	
	public void setBrush(int index) {
		if( curser instanceof BrushCurser ) {
			((BrushCurser) curser).brush.set(index);
			callListeners();
		}
	}
	
	public List<ThumbnailButton.ThumbnailData> generateBrushThumbnail(WorldId currentWorld) {
		if( curser instanceof BrushCurser )
			return ((BrushCurser) curser).brush.generateThumbnails(currentWorld);
		
		return Collections.emptyList();
	}
	
	@Override
	public void draw(RenderTarget renderTarget) {
		if( curser!=null )
			curser.draw(renderTarget);
	}
}
