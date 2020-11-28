package org.replicomment.extractor;

/**
 * DocumentedField represents the Javadoc documentation for a field in a class. It identifies
 * the field itself and the Javadoc information associated with it.
 */

public final class DocumentedField {
    /** Field name. */
    private final String name;

    /** Field type name. */
    private final String typeName;

    /** Javadoc documenting the field. */
    private String javadocFreeText;

    DocumentedField(String name, String typeName, String javadocFreeText) {
        this.name = name;
        this.typeName = typeName;
        this.javadocFreeText = javadocFreeText;
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getJavadocFreeText() {
        return javadocFreeText;
    }
}
