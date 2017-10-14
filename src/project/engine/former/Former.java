package project.engine.former;

import project.engine.data.UserJob;
import project.engine.data.VOEnvironment;

import java.util.List;

/**
 * Created by Petrukha on 06.08.2016.
 */
public abstract class Former {
    public abstract List<UserJob> form(List<UserJob> jobFlow, VOEnvironment environment,
                              FormerSettings settings);
}
