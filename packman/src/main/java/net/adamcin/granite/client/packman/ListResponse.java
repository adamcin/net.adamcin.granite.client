package net.adamcin.granite.client.packman;

import java.util.List;

public interface ListResponse extends ServiceResponse {

    List<ListResult> getResults();

    int getTotal();
}
