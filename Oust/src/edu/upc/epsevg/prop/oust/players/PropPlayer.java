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
 * Jugador de Oust usando algoritmo Minimax con poda Alpha-Beta
 */
public class PropPlayer implements IPlayer, IAuto {
    
    private String name;
    private final int MAX_DEPTH;
    private boolean timeout;
    private int nodesVisited;
    private int nodesPruned;
    
    public PropPlayer(String name, int depth) {
        this.name = name;
        this.MAX_DEPTH = depth;
        this.timeout = false;
        this.nodesVisited = 0;
        this.nodesPruned = 0;
    }
    
    @Override
    public PlayerMove move(GameStatus s) {
        timeout = false;
        nodesVisited = 0;
        nodesPruned = 0;
        
        PlayerType currentPlayer = s.getCurrentPlayer();
        List<Point> possibleFirstMoves = s.getMoves();
        
        // Si no hay movimientos posibles, pasar turno
        if (possibleFirstMoves.isEmpty()) {
            return new PlayerMove(null, nodesVisited, MAX_DEPTH, SearchType.MINIMAX);
        }
        
        // Si solo hay un movimiento posible, tomarlo directamente
        if (possibleFirstMoves.size() == 1) {
            List<Point> path = generateCompletePath(s, possibleFirstMoves.get(0));
            return new PlayerMove(path, nodesVisited, MAX_DEPTH, SearchType.MINIMAX);
        }
        
        // Buscar el mejor movimiento inicial usando Minimax
        int bestValue = Integer.MIN_VALUE;
        Point bestFirstMove = possibleFirstMoves.get(0);
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        
        System.out.println("=== EVALUANDO " + possibleFirstMoves.size() + " MOVIMIENTOS INICIALES ===");
        
        for (Point firstMove : possibleFirstMoves) {
            if (timeout) break;
            
            // Crear una copia para simular
            GameStatus newState = new GameStatus(s);
            
            // Simular solo el primer movimiento y evaluar el estado resultante
            newState.placeStone(firstMove);
            
            // Evaluar el estado después del primer movimiento
            int value = minValue(newState, 1, alpha, beta, currentPlayer);
            System.out.println("Movimiento inicial " + firstMove + " = " + value);
            
            if (value > bestValue) {
                bestValue = value;
                bestFirstMove = firstMove;
            }
            alpha = Math.max(alpha, bestValue);
        }
        
        System.out.println("MEJOR MOVIMIENTO INICIAL: " + bestFirstMove + " con valor " + bestValue);
        System.out.println("Nodos visitados: " + nodesVisited);
        System.out.println("Nodos podados: " + nodesPruned);
        
        // Generar el path completo para el mejor movimiento inicial
        List<Point> completePath = generateCompletePath(s, bestFirstMove);
        System.out.println("Path completo: " + completePath);
        System.out.println("========================");
        
        return new PlayerMove(completePath, nodesVisited, MAX_DEPTH, SearchType.MINIMAX);
    }
    
    /**
     * Genera el path completo para un movimiento inicial (igual que RandomPlayer)
     */
    private List<Point> generateCompletePath(GameStatus state, Point firstMove) {
        List<Point> path = new ArrayList<>();
        PlayerType currentPlayer = state.getCurrentPlayer();
        GameStatus aux = new GameStatus(state);
        
        // Empezar con el primer movimiento
        aux.placeStone(firstMove);
        path.add(firstMove);
        
        // Continuar mientras sea el turno del mismo jugador
        while (currentPlayer == aux.getCurrentPlayer()) {
            List<Point> moves = aux.getMoves();
            if (moves.isEmpty()) break;
            
            // Para el path, elegimos el primer movimiento disponible
            // (En una versión más avanzada, podríamos elegir el mejor)
            Point nextMove = moves.get(0);
            aux.placeStone(nextMove);
            path.add(nextMove);
        }
        
        return path;
    }
    
    private int maxValue(GameStatus state, int depth, int alpha, int beta, PlayerType maximizingPlayer) {
        nodesVisited++;

        // PRIMERO verificar condiciones terminales
        if (state.isGameOver()) {
            PlayerType winner = state.GetWinner();
            if (winner == maximizingPlayer) {
                return 1000000 - depth; // Victoria más rápida = mejor
            } else if (winner != null) {
                return -1000000 + depth; // Derrota más tardía = menos peor  
            }
            return 0; // Empate
        }

        if (timeout || depth >= MAX_DEPTH) {
            // Solo estados no terminales llegan aquí
            return Heuristica.eval(state, maximizingPlayer);
        }

        int value = Integer.MIN_VALUE;
        List<Point> moves = state.getMoves();

        for (Point move : moves) {
            if (timeout) {
                break;
            }

            GameStatus newState = new GameStatus(state);
            newState.placeStone(move);

            value = Math.max(value, minValue(newState, depth + 1, alpha, beta, maximizingPlayer));

            if (value >= beta) {
                nodesPruned++;
                return value;
            }
            alpha = Math.max(alpha, value);
        }

        return value;
    }

    private int minValue(GameStatus state, int depth, int alpha, int beta, PlayerType maximizingPlayer) {
        nodesVisited++;

        // PRIMERO verificar condiciones terminales
        if (state.isGameOver()) {
            PlayerType winner = state.GetWinner();
            if (winner == maximizingPlayer) {
                return 1000000 - depth; // Victoria más rápida = mejor
            } else if (winner != null) {
                return -1000000 + depth; // Derrota más tardía = menos peor  
            }
            return 0; // Empate
        }

        if (timeout || depth >= MAX_DEPTH) {
            // Solo estados no terminales llegan aquí
            return Heuristica.eval(state, maximizingPlayer);
        }

        int value = Integer.MAX_VALUE;
        List<Point> moves = state.getMoves();

        for (Point move : moves) {
            if (timeout) {
                break;
            }

            GameStatus newState = new GameStatus(state);
            newState.placeStone(move);

            value = Math.min(value, maxValue(newState, depth + 1, alpha, beta, maximizingPlayer));

            if (value <= alpha) {
                nodesPruned++;
                return value;
            }
            beta = Math.min(beta, value);
        }

        return value;
    }
    
    @Override
    public void timeout() {
        timeout = true;
        System.out.println("Timeout - Interrumpiendo búsqueda...");
    }
    
    @Override
    public String getName() {
        return "MinimaxPlayer(" + name + ", depth:" + MAX_DEPTH + ")";
    }
}