/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of ee-logback-level
 *
 * ee-logback-level is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ee-logback-level is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.logback.admin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.EMPTY_LIST;

/**
 * Bean that tells if a request is allowed to access the log-level admin
 * construct
 * <p>
 * Only support for ipv4
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Singleton
@LocalBean
@Startup
@Lock(LockType.READ)
public class LogLevelRemoteAccess {

    private static final Logger log = LoggerFactory.getLogger(LogLevelRemoteAccess.class);

    private static final long IPV4_MAX = 0xffffffffL;

    private List<IPRange> allowedProxyIpRanges;
    private List<IPRange> allowedAdminIpRanges;

    @PostConstruct
    public void init() {
        this.allowedProxyIpRanges = listFromEnv("X_FORWARDED_FOR");
        this.allowedAdminIpRanges = listFromEnv("ADMIN_IP");
    }

    /**
     * Resolve a remote ip address from a request processing optional
     * x-forwarded-for headers
     *
     * @param headers     Request meta data
     * @param httpRequest Request meta data
     * @return peer ip address
     */
    public String remoteIp(HttpHeaders headers, HttpServletRequest httpRequest) {
        String peer = httpRequest.getRemoteAddr();
        String xForwardedFor = headers.getHeaderString("x-forwarded-for");
        // X-Forwarded-For syntax is client-ip[, proxy-ip ...]
        log.trace("xForwardedFor header: {}", xForwardedFor);

        if (xForwardedFor != null && !xForwardedFor.isEmpty() &&
            inIpRange(peer, allowedProxyIpRanges)) {
            // Proxy connected to us, and has x-forwarded-for set
            String[] xForwardedFors = xForwardedFor.split(",");
            // Ensure (optional) proxies are in our allowed list
            int pos = xForwardedFors.length;
            while (--pos > 0) {
                String proxy = xForwardedFors[pos].trim();
                if (!inIpRange(proxy, allowedProxyIpRanges)) {
                    return proxy; // Proxy is not in allowed list - proxy is our peer
                }
            }
            return xForwardedFors[pos].trim();
        }
        return peer;
    }

    /**
     * Check if an ip (from
     * {@link #remoteIp(javax.ws.rs.core.HttpHeaders, javax.servlet.http.HttpServletRequest)}
     * is in the admin ip list
     *
     * @param ip ip number as string
     * @return if admin access is allowed
     */
    public boolean isAdminIp(String ip) {
        return inIpRange(ip, allowedAdminIpRanges);
    }

    /**
     * Convert a comma separated list of ip(-ranges) to an {@link IpRange} list
     *
     * @param env environment variable value
     * @return List of IpRanges that are in the input
     */
    private static List<IPRange> listFromEnv(String env) {
        String xForwardedFor = System.getenv(env);
        if (xForwardedFor == null || xForwardedFor.trim().isEmpty())
            return EMPTY_LIST;
        return Arrays.stream(xForwardedFor.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(LogLevelRemoteAccess::ipRange)
                .collect(Collectors.toList());
    }

    /**
     * Check if an ip address is in a list of ranges
     *
     * @param peer   ip to test for
     * @param ranges ip ranges
     * @return if ip is covered by any range
     */
    private static boolean inIpRange(String peer, List<IPRange> ranges) {
        long ip = ipOf(peer);
        return ranges.stream().anyMatch(i -> i.isInRange(ip));
    }

    /**
     * Convert a string og host/net/ranges to a list
     *
     * @param hosts ip(-range)/net list
     * @return List of ranges
     */
    private static IPRange ipRange(String hosts) {
        if (hosts.contains("-")) {
            String[] parts = hosts.split("-", 2);
            long ipMin = ipOf(parts[0]);
            long ipMax = ipOf(parts[1]);
            return new IPRange(ipMin, ipMax);
        } else if (hosts.contains("/")) {
            String[] parts = hosts.split("/", 2);
            long ip = ipOf(parts[0]);
            long net = ( IPV4_MAX << ( 32 - Integer.parseInt(parts[1]) ) ) & IPV4_MAX;
            return new IPRange(ip & net, ip | ~net);
        } else {
            long ip = ipOf(hosts);
            return new IPRange(ip, ip);
        }
    }

    /**
     * Convert a ipv4 address into a long value
     *
     * @param addr ipv4 address
     * @return 32-bit in a long
     */
    private static long ipOf(String addr) {
        if (addr.contains(".")) {
            return Arrays.stream(addr.split("\\.")).mapToInt(Integer::parseUnsignedInt).reduce(0, (l, r) -> ( l << 8 ) + r);
        } else {
            return IPV4_MAX;
        }
    }

    /**
     * Class that represents an ipv4 range
     */
    private static class IPRange {

        private final long min;
        private final long max;

        private IPRange(long min, long max) {
            this.min = min;
            this.max = max;
        }

        private boolean isInRange(long ip) {
            return ip >= min && ip <= max;
        }

        @Override
        public String toString() {
            return String.format("(%08x-%08x)", min, max);
        }

    }

}
