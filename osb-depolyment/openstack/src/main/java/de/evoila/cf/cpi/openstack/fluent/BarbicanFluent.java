package de.evoila.cf.cpi.openstack.fluent;

import de.evoila.cf.cpi.openstack.fluent.connection.OpenstackConnectionFactory;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.heat.StackService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Created by reneschollmeyer on 02.08.17.
 */
@Component
@ConditionalOnProperty(prefix="openstack", name={"endpoint"}, havingValue="")
public class BarbicanFluent {

    private OSClient client() { return OpenstackConnectionFactory.connection(); }

    private StackService list() { return null; }

}
