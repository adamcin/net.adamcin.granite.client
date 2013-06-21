package net.adamcin.granite.client.packman;

public interface PackIdFilter {
    boolean includes(PackId packId);
}
