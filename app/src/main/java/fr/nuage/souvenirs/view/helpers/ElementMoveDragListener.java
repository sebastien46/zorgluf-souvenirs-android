package fr.nuage.souvenirs.view.helpers;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Point;
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.LifecycleOwner;

import java.util.UUID;

import fr.nuage.souvenirs.R;
import fr.nuage.souvenirs.model.ImageElement;
import fr.nuage.souvenirs.view.EditTextElementDialogFragment;
import fr.nuage.souvenirs.view.ImageActionModeCallback;
import fr.nuage.souvenirs.view.TextActionModeCallback;
import fr.nuage.souvenirs.viewmodel.ElementViewModel;
import fr.nuage.souvenirs.viewmodel.ImageElementViewModel;
import fr.nuage.souvenirs.viewmodel.PageViewModel;
import fr.nuage.souvenirs.viewmodel.TextElementViewModel;

public class ElementMoveDragListener implements View.OnDragListener, View.OnLongClickListener, View.OnClickListener, View.OnTouchListener {

    private final static String SWITCH_DRAG = "SWITCH_DRAG";
    private final static String MOVE_DRAG = "MOVE_DRAG";
    private final static String RESIZE_DRAG_RIGHT_BOTTOM = "RESIZE_DRAG_RIGHT_BOTTOM";
    private final static String RESIZE_DRAG_LEFT_TOP = "RESIZE_DRAG_LEFT_TOP";
    private final static String PAN_DRAG = "PAN_DRAG";

    private PageViewModel pageVM;
    private ElementViewModel elVM;
    private GestureDetectorCompat gestureDetector;
    private static int activateMoveViewId = 0;
    private static int activatePanViewId = 0;
    private float initialX, initialY;

    public ElementMoveDragListener(PageViewModel page, ElementViewModel el, AppCompatActivity activity) {
        pageVM = page;
        elVM = el;
        gestureDetector = new GestureDetectorCompat(activity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent event) {
                if (elVM instanceof TextElementViewModel) {
                    EditTextElementDialogFragment.newInstance((TextElementViewModel) elVM).show(activity.getSupportFragmentManager(), "");
                    return true;
                }
                return false;
            }
        });

    }

    @Override
    public boolean onDrag(View view, DragEvent dragEvent) {

        String dragType = (String)dragEvent.getLocalState();
        if (dragType.equals(SWITCH_DRAG)) {
            //handle switch elements drag action
            int action = dragEvent.getAction();
            switch(action) {
                case DragEvent.ACTION_DRAG_STARTED:
                case DragEvent.ACTION_DRAG_EXITED:
                    view.setAlpha((float)0.5);
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                case DragEvent.ACTION_DRAG_ENDED:
                    view.setAlpha(1);
                    return true;
                case DragEvent.ACTION_DRAG_LOCATION:
                    return true;
                case DragEvent.ACTION_DROP:
                    // Gets the page id to swap
                    ClipData.Item item = dragEvent.getClipData().getItemAt(0);
                    UUID oriElementUUID = UUID.fromString((String)item.getText());
                    UUID destElementUUID = elVM.getId().getValue();
                    pageVM.swapElements(oriElementUUID,destElementUUID);
                    return true;
                default:
                    break;
            }
        }

        if (dragType.equals(MOVE_DRAG) || (dragType.startsWith("RESIZE_DRAG"))) {
            //handle move element drag action
            int action = dragEvent.getAction();
            switch(action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    //capture all
                    initialX = dragEvent.getX();
                    initialY = dragEvent.getY();
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    if (activateMoveViewId == 0) {
                        activateMoveViewId = view.getId();
                        return true;
                    }
                    return false;
                case DragEvent.ACTION_DRAG_LOCATION:
                    if (activateMoveViewId == view.getId()) {
                        if (dragType.equals(MOVE_DRAG)) {
                            //move image
                            float x = dragEvent.getX();
                            float y = dragEvent.getY();
                            view.setX(view.getX()+x- initialX);
                            view.setY(view.getY()+y- initialY);
                        }
                        if (dragType.equals(RESIZE_DRAG_RIGHT_BOTTOM)) {
                            //change height/width of image
                            float x = dragEvent.getX();
                            float y = dragEvent.getY();
                            view.setRight(Math.round(view.getRight()+x- initialX));
                            view.setBottom(Math.round(view.getBottom()+y- initialY));
                            initialX = x;
                            initialY = y;
                        }
                        if (dragType.equals(RESIZE_DRAG_LEFT_TOP)) {
                            //change height/width of image
                            float x = dragEvent.getX();
                            float y = dragEvent.getY();
                            view.setLeft(Math.round(view.getLeft()+x- initialX));
                            view.setTop(Math.round(view.getTop()+y- initialY));
                            initialX = x;
                            initialY = y;
                        }
                    }
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    if (activateMoveViewId == view.getId()) {
                        activateMoveViewId = 0;
                        //set new element coordinate
                        int parentX = ((View)view.getParent()).getWidth();
                        int parentY = ((View)view.getParent()).getHeight();
                        int top = Math.round(view.getY()/parentY*100);
                        int left = Math.round(view.getX()/parentX*100);
                        int bottom = Math.round((view.getY()+view.getHeight())/parentY*100);
                        int right = Math.round((view.getX()+view.getWidth())/parentX*100);
                        elVM.setPosition(top,left,bottom,right);
                        elVM.bringToFront();
                    }
                    return true;
                default:
                    break;
            }
        }

        if (dragType.equals(PAN_DRAG)) {
            //handle pan element drag action
            int action = dragEvent.getAction();
            switch(action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    //capture all
                    initialX = dragEvent.getX();
                    initialY = dragEvent.getY();
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    if (activatePanViewId == 0) {
                        activatePanViewId = view.getId();
                        return true;
                    }
                    return false;
                case DragEvent.ACTION_DRAG_LOCATION:
                    if (activatePanViewId == view.getId()) {
                        //pan image
                        float x = dragEvent.getX();
                        float y = dragEvent.getY();
                        view.setScrollX((int)(initialX-x));
                        view.setScrollY((int)(initialY-y));
                    }
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    if (activatePanViewId == view.getId()) {
                        activatePanViewId = 0;
                        //set new element offset
                        ImageElementViewModel ei = (ImageElementViewModel)elVM;
                        ei.setOffset((int)(view.getScrollX()/(float)view.getWidth()*100)+ei.getOffsetX().getValue(),(int)(view.getScrollY()/(float)view.getHeight()*100)+ei.getOffsetY().getValue());
                        view.setScrollX(0);
                        view.setScrollY(0);
                    }
                    return true;
                default:
                    break;
            }
        }

        return false;
    }

    @Override
    public boolean onLongClick(View view) {
        if (!pageVM.getPaintMode()) {
            if (view.isSelected()){
            } else {
                ClipData dragData = ClipData.newPlainText(ClipDescription.MIMETYPE_TEXT_PLAIN, view.getTag().toString());
                view.startDrag(dragData, new View.DragShadowBuilder(view), SWITCH_DRAG, 0);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        if (!pageVM.getPaintMode()) {
            if (view.isSelected()) {
            } else {
                if (elVM.getClass().equals(ImageElementViewModel.class)) {
                    elVM.setSelected(true);
                }
                if (elVM.getClass().equals(TextElementViewModel.class)) {
                    elVM.setSelected(true);
                }
            }
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (!pageVM.getPaintMode()) {
            if (gestureDetector != null) {
                if (gestureDetector.onTouchEvent(motionEvent)) {
                    return true;
                }
            }
            if (view.isSelected()) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    //check if resize
                    int resize_radius = (int) (view.getResources().getDimension(R.dimen.selected_circle_ctl));
                    String dragAction = MOVE_DRAG;
                    if (motionEvent.getX() < resize_radius) {
                        if (motionEvent.getY() < resize_radius) {
                            dragAction = RESIZE_DRAG_LEFT_TOP;
                        }
                    } else if ((view.getWidth() - motionEvent.getX()) < resize_radius) {
                        if ((view.getHeight() - motionEvent.getY()) < resize_radius) {
                            dragAction = RESIZE_DRAG_RIGHT_BOTTOM;
                        }
                    }
                    if ((elVM instanceof ImageElementViewModel) && (((ImageElementViewModel)elVM).getTransformType().getValue() == ImageElement.ZOOM_OFFSET)) {
                        dragAction = PAN_DRAG;
                    }
                    //do drag
                    //move view to front before drag
                    view.bringToFront();
                    //start drag
                    view.startDrag(null, new View.DragShadowBuilder() {
                        @Override
                        public void onProvideShadowMetrics(Point outShadowSize, Point outShadowTouchPoint) {
                            outShadowSize.set(1, 1);
                            outShadowTouchPoint.set(0, 0);
                        }
                    }, dragAction, 0);
                    return true;
                }
            }
        }
        return false;
    }


}
