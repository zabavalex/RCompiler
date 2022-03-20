public class Symbol {
    private String name;
    private String type;
    private String id;

    public Symbol(String name, String type, String id) {
        this.name = name;
        this.type = type;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "ID:" + this.getId() + " Name:" + this.getName() + " Type:" + this.getType();
    }
}
