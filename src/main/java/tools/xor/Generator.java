package tools.xor;

import javax.xml.bind.annotation.XmlElement;

public class Generator {
    private String displayName;
    private String className;
    private String type;
    private String description;
    private String htmlHelp;

    public String getDisplayName ()
    {
        return displayName;
    }

    public void setDisplayName (String displayName)
    {
        this.displayName = displayName;
    }

    public String getClassName ()
    {
        return className;
    }

    public void setClassName (String className)
    {
        this.className = className;
    }

    public String getType ()
    {
        return type;
    }

    public void setType (String type)
    {
        this.type = type;
    }

    public String getDescription ()
    {
        return description;
    }

    public void setDescription (String description)
    {
        this.description = description;
    }

    public String getHtmlHelp ()
    {
        return htmlHelp;
    }

    public void setHtmlHelp (String htmlHelp)
    {
        this.htmlHelp = htmlHelp;
    }
}
