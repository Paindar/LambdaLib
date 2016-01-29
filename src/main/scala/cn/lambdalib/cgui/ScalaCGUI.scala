/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib.cgui

import cn.lambdalib.cgui.gui.component.Component
import cn.lambdalib.cgui.gui.Widget
import cn.lambdalib.cgui.gui.event.{IGuiEventHandler, GuiEvent}

import scala.reflect.ClassTag

class RichWidget(val w: Widget) extends AnyVal {

  def listens[T <: GuiEvent](handler: (Widget, T) => Any, priority: Int = 0)(implicit evidence: ClassTag[T]): Widget = {
    w.listen[T](evidence.runtimeClass.asInstanceOf[Class[T]], new IGuiEventHandler[T] {
      override def handleEvent(w: Widget, event: T) = {
        handler(w, event)
      }
    }, priority)
    w
  }

  def listens[T <: GuiEvent](handler: T => Any)(implicit evidence: ClassTag[T]): Widget = {
    listens((_, e: T) => handler(e))
    w
  }

  def listens[T <: GuiEvent](handler: () => Any)(implicit evidence: ClassTag[T]): Widget = {
    listens((_, _: T) => handler())
    w
  }

  def :+(add: Widget): Unit = w.addWidget(add)

  def :+(pair: (String, Widget)): Unit = w.addWidget(pair._1, pair._2)

  def :+(c: Component): Unit = w.addComponent(c)

}

class RichComponent(val c: Component) extends AnyVal {
  def listens[T <: GuiEvent](handler: (Widget, T) => Any)(implicit tag: ClassTag[T]): Unit = {
    c.listen[T](tag.runtimeClass.asInstanceOf[Class[T]], new IGuiEventHandler[T] {
      override def handleEvent(w: Widget, e: T) = handler(w, e)
    })
  }

  def listens[T <: GuiEvent](handler: T => Any)(implicit tag: ClassTag[T]): Unit = {
    listens((_, e:T) => handler(e))
  }

  def listens[T <: GuiEvent](handler: () => Any)(implicit tag: ClassTag[T]): Unit = {
    listens((_, _:T) => handler())
  }
}

/**
  * CGUI scala extensions to reduce syntax burden.
  */
object ScalaCGUI {

  implicit def toWrapper(w: Widget): RichWidget = new RichWidget(w)

  implicit def toComponentWrapper(c: Component): RichComponent = new RichComponent(c)

}