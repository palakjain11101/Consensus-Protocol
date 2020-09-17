import java.util.StringTokenizer;
import java.util.*;

/**
 * A scanner and parser for requests.
 */

class Tokenizer {
    Tokenizer() { ; }

    /**
     * Parses requests.
     */
    Token getToken(String req) {
        StringTokenizer sTokenizer = new StringTokenizer(req);
        if (!(sTokenizer.hasMoreTokens()))
            return null;
        String firstToken = sTokenizer.nextToken();
        if (firstToken.equals("JOIN")) {
            if (sTokenizer.hasMoreTokens())
                return new JoinToken(req, sTokenizer.nextToken());
            else
                return null;
        }
        if (firstToken.equals("DETAILS")) {
            List<String> ports = new ArrayList<>();
            while (sTokenizer.hasMoreTokens())
                ports.add(sTokenizer.nextToken());
            return new DetailsToken(req, ports);
        }
        if (firstToken.equals("VOTE_OPTIONS")) {
            List<String> vote_options = new ArrayList<>();
            while (sTokenizer.hasMoreTokens())
                vote_options.add(sTokenizer.nextToken());
            return new VoteToken(req, vote_options);
        }
        if (firstToken.equals("OUTCOME")) {
            String outcome = sTokenizer.nextToken();
            List<String> ports = new ArrayList<>();
            while (sTokenizer.hasMoreTokens())
                ports.add(sTokenizer.nextToken());
            return new OutcomeToken(req, outcome, ports);
        }
        if (firstToken.equals("EXIT"))
            return new ExitToken(req);
        return null; // Ignore request..
    }
}


/**
 * The Token Prototype.
 */
abstract class Token {
    String _req;
}

/**
 * Syntax: JOIN &lt;name&gt;
 */
class JoinToken extends Token {
    String _name;
    JoinToken(String req, String name) {
        this._req = req;
        this._name = name;
    }
}

/**
 * Syntax: YELL &lt;msg&gt;
 */
class DetailsToken extends Token {
    List<String> _details;
    DetailsToken(String req, List<String> details) {
        this._req = req;
        this._details = details;
    }
}

/**
 * Syntax: YELL &lt;msg&gt;
 */
class VoteToken extends Token {
    List<String> _details;
    VoteToken(String req, List<String> details) {
        this._req = req;
        this._details = details;
    }
}

/**
 * Syntax: EXIT
 */
class OutcomeToken extends Token {
    List<String> _ports;
    String _outcome;
    OutcomeToken(String req, String outcome, List<String> ports) {
        this._req = req;
        this._outcome = outcome;
        this._ports = ports;
    }
}

class ExitToken extends Token {
    ExitToken(String req) { this._req = req; }
}