package github.clone_code_detection.role;

public enum Role {
    Index("index"), Query("query");

    private final String roleName;

    Role(String name) {
        this.roleName = name;
    }
}
