/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib.vis.animation;

/**
 * Base interface for time-based animation. Provide a time point, and the animation modifies the value based on the parameter.
 * @author WeAthFolD
 */
public abstract class Animation {
    
    public boolean disposed = false;
    
    /**
     * Perform the animation at the given timepoint.
     */
    public abstract void perform(double timePoint);
    
    /**
     * Callback when animation is started.
     */
    public void onStarted() {}
    
    /**
     * Callback when animation is ended.
     */
    public void onEnded() {}
    
}
