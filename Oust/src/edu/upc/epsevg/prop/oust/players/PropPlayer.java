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
 * Jugador de Oust usando Minimax con poda Alpha-Beta
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
        
        List<Point> possibleMoves = s.getMoves();
        
        // Sin movimientos → pasar
        if (possibleMoves.isEmpty()) {
            return new PlayerMove(null, nodesVisited, MAX_DEPTH, SearchType.MINIMAX);
        }
        
        PlayerType currentPlayer = s.getCurrentPlayer();
        int bestValue = Integer.MIN_VALUE;
        List<Point> bestPath = null;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        
        // Evaluar cada movimiento inicial
        for (Point firstMove : possibleMoves) {
            if (timeout) break;
            
            // Evaluar este movimiento construyendo el path óptimo
            PathEvaluation eval = evaluarMovimiento(s, firstMove, currentPlayer, 0, alpha, beta);
            
            if (eval.valor > bestValue) {
                bestValue = eval.valor;
                bestPath = eval.path;
            }
            
            alpha = Math.max(alpha, bestValue);
            if (bestValue >= beta) break;
        }
        
        return new PlayerMove(bestPath, nodesVisited, MAX_DEPTH, SearchType.MINIMAX);
    }
    
    /**
     * Evalúa un movimiento y construye el mejor path desde ese punto
     * Explora TODAS las opciones cuando hay capturas encadenadas
     */
    private PathEvaluation evaluarMovimiento(GameStatus state, Point move, PlayerType player, 
                                            int depth, int alpha, int beta) {
        GameStatus newState = new GameStatus(state);
        List<Point> path = new ArrayList<>();
        path.add(move);
        newState.placeStone(move);
        
        // Si sigue siendo nuestro turno (captura), debemos elegir continuación
        if (player == newState.getCurrentPlayer() && !newState.isGameOver()) {
            List<Point> continuaciones = newState.getMoves();
            
            if (!continuaciones.isEmpty()) {
                // IMPORTANTE: Explorar TODAS las continuaciones y elegir la mejor
                PathEvaluation mejorContinuacion = null;
                int mejorValor = Integer.MIN_VALUE;
                
                for (Point cont : continuaciones) {
                    PathEvaluation eval = evaluarMovimiento(newState, cont, player, depth + 1, alpha, beta);
                    
                    if (eval.valor > mejorValor) {
                        mejorValor = eval.valor;
                        mejorContinuacion = eval;
                    }
                }
                
                // Combinar paths
                path.addAll(mejorContinuacion.path);
                newState = mejorContinuacion.state;
            }
        }
        
        // Evaluar el estado final del path completo
        int valor = minimax(newState, depth + 1, alpha, beta, player);
        
        return new PathEvaluation(path, valor, newState);
    }
    
    /**
     * Algoritmo Minimax con poda Alpha-Beta
     */
    private int minimax(GameStatus state, int depth, int alpha, int beta, PlayerType maximizingPlayer) {
        nodesVisited++;
        
        // Condición terminal: juego terminado
        if (state.isGameOver()) {
            PlayerType winner = state.GetWinner();
            if (winner == maximizingPlayer) {
                return 1000000 - depth;
            } else if (winner != null) {
                return -1000000 + depth;
            }
            return 0;
        }
        
        // Condición terminal: profundidad máxima o timeout
        if (timeout || depth >= MAX_DEPTH) {
            return Heuristica.eval(state, maximizingPlayer);
        }
        
        PlayerType currentPlayer = state.getCurrentPlayer();
        List<Point> moves = state.getMoves();
        
        // Sin movimientos → pasar turno
        if (moves.isEmpty()) {
            return minimax(state, depth + 1, alpha, beta, maximizingPlayer);
        }
        
        boolean maximizing = (currentPlayer == maximizingPlayer);
        
        if (maximizing) {
            // Nodo MAX
            int value = Integer.MIN_VALUE;
            
            for (Point move : moves) {
                if (timeout) break;
                
                // Construir el path completo para este movimiento
                GameStatus newState = completarJugadaOptima(state, move, currentPlayer);
                
                value = Math.max(value, minimax(newState, depth + 1, alpha, beta, maximizingPlayer));
                
                if (value >= beta) return value; // Poda beta
                alpha = Math.max(alpha, value);
            }
            return value;
            
        } else {
            // Nodo MIN
            int value = Integer.MAX_VALUE;
            
            for (Point move : moves) {
                if (timeout) break;
                
                // Construir el path completo para este movimiento
                GameStatus newState = completarJugadaOptima(state, move, currentPlayer);
                
                value = Math.min(value, minimax(newState, depth + 1, alpha, beta, maximizingPlayer));
                
                if (value <= alpha) return value; // Poda alpha
                beta = Math.min(beta, value);
            }
            return value;
        }
    }
    
    /**
     * Completa una jugada eligiendo las mejores continuaciones cuando hay capturas
     */
    private GameStatus completarJugadaOptima(GameStatus state, Point firstMove, PlayerType player) {
        GameStatus newState = new GameStatus(state);
        newState.placeStone(firstMove);
        
        // Mientras siga siendo nuestro turno, elegir mejor continuación
        while (player == newState.getCurrentPlayer() && !newState.isGameOver()) {
            List<Point> continuaciones = newState.getMoves();
            if (continuaciones.isEmpty()) break;
            
            // Si solo hay una opción, tomarla
            if (continuaciones.size() == 1) {
                newState.placeStone(continuaciones.get(0));
            } else {
                // Evaluar cada continuación y elegir la mejor
                Point mejorCont = continuaciones.get(0);
                int mejorValor = Integer.MIN_VALUE;
                
                for (Point cont : continuaciones) {
                    GameStatus temp = new GameStatus(newState);
                    temp.placeStone(cont);
                    int valor = Heuristica.eval(temp, player);
                    
                    if (valor > mejorValor) {
                        mejorValor = valor;
                        mejorCont = cont;
                    }
                }
                
                newState.placeStone(mejorCont);
            }
        }
        
        return newState;
    }
    
    @Override
    public void timeout() {
        timeout = true;
    }
    
    @Override
    public String getName() {
        return "PropPlayer(" + name + ", d:" + MAX_DEPTH + ")";
    }
    
    /**
     * Clase auxiliar para almacenar evaluación de un path
     */
    private static class PathEvaluation {
        List<Point> path;
        int valor;
        GameStatus state;
        
        PathEvaluation(List<Point> path, int valor, GameStatus state) {
            this.path = path;
            this.valor = valor;
            this.state = state;
        }
    }
}