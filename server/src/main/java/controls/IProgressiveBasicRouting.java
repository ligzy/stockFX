package controls;

import pojo.NavigationDescriptor;

/**
 * Created by salterok on 16.05.2015.
 */
public interface IProgressiveBasicRouting {
    void setNextCommand(Runnable command);
    void setPrevCommand(Runnable command);
    void setNavigation(NavigationDescriptor[] navs);
}
