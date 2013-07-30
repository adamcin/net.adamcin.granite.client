package net.adamcin.granite.client.packman;

public interface ListResult {
    PackId getPackId();
    boolean isHasSnapshot();
    boolean isNeedsRewrap();
}
