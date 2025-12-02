package edu.upc.epsevg.prop.oust.players;

import edu.upc.epsevg.prop.oust.GameStatus;
import edu.upc.epsevg.prop.oust.IAuto;
import edu.upc.epsevg.prop.oust.IPlayer;
import edu.upc.epsevg.prop.oust.PlayerMove;
import edu.upc.epsevg.prop.oust.PlayerType;
import edu.upc.epsevg.prop.oust.SearchType;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Jugador Minimax con Alpha-Beta para Oust
 */
public class PropPlayer implements IPlayer, IAuto {
    
    private String name;
    private final int MAX_DEPTH;
    private boolean timeout;
    private int nodesVisited;
    
    public PropPlayer(String name, int depth) {
        this.name = name;
        this.MAX_DEPTH = depth;
    }
    
    @Override
    public PlayerMove move(GameStatus s) {
        timeout = false;
        nodesVisited = 0;
        
        List<Point> moves = s.getMoves();
        if (moves.isEmpty()) {
            return new PlayerMove(null, nodesVisited, MAX_DEPTH, SearchType.MINIMAX);
        }
        
        PlayerType player = s.getCurrentPlayer();
        int best = Integer.MIN_VALUE;
        List<Point> bestPath = null;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        
        for (Point m : moves) {
            if (timeout) break;
            
            PathEval eval = evalPath(s, m, player, 0, alpha, beta);
            
            if (eval.valor > best) {
                best = eval.valor;
                bestPath = eval.path;
            }
            
            alpha = Math.max(alpha, best);
            if (best >= beta) break;
        }
        
        return new PlayerMove(bestPath, nodesVisited, MAX_DEPTH, SearchType.MINIMAX);
    }
    
    /**
     * Evalúa un movimiento construyendo el mejor path posible
     */
    private PathEval evalPath(GameStatus s, Point m, PlayerType p, int depth, int alpha, int beta) {
        GameStatus ns = new GameStatus(s);
        List<Point> path = new ArrayList<>();
        path.add(m);
        ns.placeStone(m);
        
        // Si hay capturas encadenadas, explorar todas las opciones
        if (p == ns.getCurrentPlayer() && !ns.isGameOver()) {
            List<Point> conts = ns.getMoves();
            
            if (!conts.isEmpty()) {
                PathEval mejor = null;
                int mejorVal = Integer.MIN_VALUE;
                
                for (Point c : conts) {
                    PathEval e = evalPath(ns, c, p, depth + 1, alpha, beta);
                    if (e.valor > mejorVal) {
                        mejorVal = e.valor;
                        mejor = e;
                    }
                }
                
                path.addAll(mejor.path);
                ns = mejor.state;
            }
        }
        
        int valor = minimax(ns, depth + 1, alpha, beta, p);
        return new PathEval(path, valor, ns);
    }
    
    /**
     * Minimax con Alpha-Beta
     */
    private int minimax(GameStatus s, int depth, int alpha, int beta, PlayerType maxP) {
        nodesVisited++;
        
        if (s.isGameOver()) {
            PlayerType w = s.GetWinner();
            if (w == maxP) return 1000000 - depth;
            if (w != null) return -1000000 + depth;
            return 0;
        }
        
        if (timeout || depth >= MAX_DEPTH) {
            return Heuristica.eval(s, maxP);
        }
        
        List<Point> moves = s.getMoves();
        if (moves.isEmpty()) {
            return minimax(s, depth + 1, alpha, beta, maxP);
        }
        
        PlayerType curr = s.getCurrentPlayer();
        boolean max = (curr == maxP);
        int val = max ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        
        for (Point m : moves) {
            if (timeout) break;
            
            GameStatus ns = completar(s, m, curr);
            int v = minimax(ns, depth + 1, alpha, beta, maxP);
            
            if (max) {
                val = Math.max(val, v);
                if (val >= beta) return val;
                alpha = Math.max(alpha, val);
            } else {
                val = Math.min(val, v);
                if (val <= alpha) return val;
                beta = Math.min(beta, val);
            }
        }
        
        return val;
    }
    
    /**
     * Completa una jugada con capturas encadenadas
     */
    private GameStatus completar(GameStatus s, Point m, PlayerType p) {
        GameStatus ns = new GameStatus(s);
        ns.placeStone(m);
        
        while (p == ns.getCurrentPlayer() && !ns.isGameOver()) {
            List<Point> conts = ns.getMoves();
            if (conts.isEmpty()) break;
            
            if (conts.size() == 1) {
                ns.placeStone(conts.get(0));
            } else {
                Point mejor = conts.get(0);
                int mejorV = Integer.MIN_VALUE;
                
                for (Point c : conts) {
                    GameStatus tmp = new GameStatus(ns);
                    tmp.placeStone(c);
                    int v = Heuristica.eval(tmp, p);
                    if (v > mejorV) {
                        mejorV = v;
                        mejor = c;
                    }
                }
                ns.placeStone(mejor);
            }
        }
        
        return ns;
    }
    
    @Override
    public void timeout() {
        timeout = true;
    }
    
    @Override
    public String getName() {
        return "PropPlayer(" + name + ")";
    }
    
    private static class PathEval {
        List<Point> path;
        int valor;
        GameStatus state;
        
        PathEval(List<Point> p, int v, GameStatus s) {
            this.path = p;
            this.valor = v;
            this.state = s;
        }
    }
}