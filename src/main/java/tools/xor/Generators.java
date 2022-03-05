package tools.xor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;

@XmlRootElement(name="Generators")
public class Generators {
    private Set<Generator> generators;

    @XmlElement(name="generator")
    public Set<Generator> getGenerators() {
        return generators;
    }

    public void setGenerators(Set<Generator> value) {
        this.generators = value;
    }
}
