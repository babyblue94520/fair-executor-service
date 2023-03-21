package pers.clare.concurrent.vo;

import java.util.Objects;

public class KeyBean {
    private final Integer id;
    private final String name;

    public KeyBean(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyBean keyBean = (KeyBean) o;
        return Objects.equals(id, keyBean.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "KeyBean{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
