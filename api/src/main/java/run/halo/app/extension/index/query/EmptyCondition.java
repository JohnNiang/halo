package run.halo.app.extension.index.query;

public record EmptyCondition() implements Condition {

    @Override
    public String toString() {
        return "EMPTY";
    }
}
